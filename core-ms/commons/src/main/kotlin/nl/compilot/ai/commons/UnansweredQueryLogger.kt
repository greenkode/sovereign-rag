package nl.compilot.ai.commons

/**
 * Interface for logging unanswered queries
 * Implemented in client module, used by core-ai module
 */
interface UnansweredQueryLogger {
    /**
     * Log an unanswered query for analytics
     */
    fun logUnansweredQuery(
        query: String,
        response: String?,
        bestConfidence: Double?,
        usedGeneralKnowledge: Boolean,
        language: String? = null
    )
}
