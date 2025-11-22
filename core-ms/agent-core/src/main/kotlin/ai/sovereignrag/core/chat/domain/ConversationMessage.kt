package ai.sovereignrag.core.chat.domain

import java.time.LocalDateTime

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
