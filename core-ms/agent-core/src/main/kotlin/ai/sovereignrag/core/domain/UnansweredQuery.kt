package ai.sovereignrag.core.domain

import java.time.LocalDateTime

/**
 * Represents a user query that couldn't be answered from the knowledge base
 */
data class UnansweredQuery(
    val id: String,
    val query: String,
    val response: String?,  // The response that was given to the user
    val timestamp: LocalDateTime,
    val confidence: Double?,  // Best match confidence score, if any
    val usedGeneralKnowledge: Boolean,
    val reviewed: Boolean = false,
    val notes: String? = null
)
