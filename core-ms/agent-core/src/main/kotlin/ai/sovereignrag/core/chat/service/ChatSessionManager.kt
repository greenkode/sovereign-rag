package ai.sovereignrag.core.chat.service

import nl.compilot.ai.chat.domain.ChatSession
import nl.compilot.ai.config.CompilotCache
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Chat session manager with Spring Cache abstraction
 * Works with both Redis and in-memory cache based on spring.cache.type
 */
@Service
class ChatSessionManager {

    /**
     * Create new chat session and cache it
     */
    @CachePut(cacheNames = [CompilotCache.CHAT_SESSION], key = "#result.sessionId")
    fun createNewChatSession(persona: String = "customer_service", language: String? = null): ChatSession {
        val sessionId = UUID.randomUUID().toString()
        val session = ChatSession(
            sessionId = sessionId,
            persona = persona,
            language = language
        )
        logger.info { "Created chat session: $sessionId with persona: $persona, language: $language" }
        return session
    }

    /**
     * Find active chat session from cache
     * Returns null if not found or expired
     */
    @Cacheable(cacheNames = [CompilotCache.CHAT_SESSION], key = "#sessionId", unless = "#result == null")
    fun findActiveChatSession(sessionId: String): ChatSession? {
        // Cache miss - session doesn't exist or expired
        logger.debug { "Cache miss for session: $sessionId" }
        return null
    }

    /**
     * Update session in cache (e.g., after updating lastAccessedAt)
     */
    @CachePut(cacheNames = [CompilotCache.CHAT_SESSION], key = "#session.sessionId")
    fun updateChatSession(session: ChatSession): ChatSession {
        logger.debug { "Updated chat session: ${session.sessionId}" }
        return session
    }

    /**
     * Get and update session access time
     */
    fun getAndUpdateSession(sessionId: String): ChatSession? {
        val session = findActiveChatSession(sessionId) ?: return null
        session.lastAccessedAt = LocalDateTime.now()
        return updateChatSession(session)
    }

    /**
     * Terminate chat session and remove from cache
     */
    @CacheEvict(cacheNames = [CompilotCache.CHAT_SESSION], key = "#sessionId")
    fun terminateChatSession(sessionId: String) {
        logger.info { "Terminated chat session: $sessionId" }
    }

    /**
     * Clear all sessions from cache
     */
    @CacheEvict(cacheNames = [CompilotCache.CHAT_SESSION], allEntries = true)
    fun clearAllSessions() {
        logger.info { "Cleared all chat sessions from cache" }
    }

    /**
     * Cleanup expired sessions
     * Note: With Redis TTL, this is handled automatically
     * For in-memory cache, this is a manual cleanup process
     */
    fun cleanupExpiredChatSessions(timeoutMinutes: Long) {
        // Redis automatically handles TTL expiration
        // For in-memory cache, Spring doesn't provide built-in TTL,
        // so expired sessions will remain until manually evicted
        logger.debug { "Cleanup called (Redis handles TTL automatically)" }
    }
}
