package ai.sovereignrag.commons.agent

/**
 * Configuration for an Agent instance.
 *
 * Defines behavioral parameters like:
 * - RAG (Retrieval-Augmented Generation) settings
 * - General knowledge fallback
 * - Confidence thresholds
 * - Timeout settings
 * - Feature flags
 */
data class AgentConfig(
    /**
     * Enable RAG (Retrieval-Augmented Generation) for factual responses
     */
    val enableRAG: Boolean = true,

    /**
     * Enable general knowledge responses when no RAG context is found
     */
    val enableGeneralKnowledge: Boolean = true,

    /**
     * Minimum confidence threshold for accepting responses
     * Below this, the agent may request escalation or indicate uncertainty
     */
    val minConfidence: Double = 0.5,

    /**
     * Confidence threshold considered "low" (triggers warnings)
     */
    val lowConfidenceThreshold: Double = 0.5,

    /**
     * Number of search results to retrieve from RAG
     */
    val numResults: Int = 5,

    /**
     * Maximum number of results to consider
     */
    val maxResults: Int = 50,

    /**
     * Enable re-ranking for improved RAG accuracy
     */
    val useReranking: Boolean = false,

    /**
     * Timeout for LLM requests (milliseconds)
     */
    val timeoutMs: Long = 120_000,

    /**
     * Enable guardrails (input/output validation)
     */
    val enableGuardrails: Boolean = true,

    /**
     * Enable tools/function calling
     */
    val enableTools: Boolean = true,

    /**
     * Session timeout (minutes)
     */
    val sessionTimeoutMinutes: Int = 30,

    /**
     * Additional configuration (extensibility)
     */
    val additionalConfig: Map<String, Any> = emptyMap()
)
