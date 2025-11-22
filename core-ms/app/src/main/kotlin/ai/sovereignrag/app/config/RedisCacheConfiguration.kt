package ai.sovereignrag.app.config

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import kotlin.to

private val logger = KotlinLogging.logger {}

/**
 * Redis cache configuration for chat sessions
 * Activated when spring.cache.type=redis
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "redis", matchIfMissing = false)
open class RedisCacheConfiguration {

    @Bean
    open fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        logger.info { "Initializing Redis cache manager for chat sessions" }

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(JdkSerializationRedisSerializer()))

        // Configure cache-specific TTLs
        val cacheConfigurations = _root_ide_package_.kotlin.collections.mapOf(
            SovereignRagCache.CHAT_SESSION to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            SovereignRagCache.CHAT_MESSAGES to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            SovereignRagCache.CHAT_RESPONSES to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            SovereignRagCache.AGENT_CONFIG to defaultConfig.entryTtl(Duration.ofHours(24))
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}

/**
 * Simple in-memory cache configuration (fallback/development)
 * Activated when spring.cache.type=simple or not set
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "simple", matchIfMissing = true)
open class SimpleCacheConfiguration {

    @Bean
    open fun cacheManager(): CacheManager {
        logger.info { "Initializing simple in-memory cache manager for chat sessions" }
        return ConcurrentMapCacheManager(
            SovereignRagCache.CHAT_SESSION,
            SovereignRagCache.CHAT_MESSAGES,
            SovereignRagCache.CHAT_RESPONSES,
            SovereignRagCache.AGENT_CONFIG
        )
    }
}

/**
 * Cache names for Sovereign RAG
 */
object SovereignRagCache {
    const val CHAT_SESSION = "chat_session"
    const val CHAT_MESSAGES = "chat_messages"
    const val CHAT_RESPONSES = "chat_responses"
    const val AGENT_CONFIG = "agentConfig"
}
