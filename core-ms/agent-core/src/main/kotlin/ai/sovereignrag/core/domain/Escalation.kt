package ai.sovereignrag.core.domain

import java.time.LocalDateTime

/**
 * Represents a conversation escalated to a human agent
 */
data class Escalation(
    val id: String,
    val sessionId: String,
    val reason: String,
    val userEmail: String,
    val userName: String? = null,
    val userPhone: String? = null,
    val userMessage: String? = null,
    val conversationMessages: List<ConversationMessage>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val reviewed: Boolean = false,
    val reviewedAt: LocalDateTime? = null,
    val reviewNotes: String? = null,
    val emailSent: Boolean = false,
    val emailSentAt: LocalDateTime? = null,
    val language: String? = null,
    val persona: String
)

/**
 * Represents a single message in a conversation
 */
data class ConversationMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: LocalDateTime,
    val sources: List<String> = emptyList(),
    val usedGeneralKnowledge: Boolean = false
)
