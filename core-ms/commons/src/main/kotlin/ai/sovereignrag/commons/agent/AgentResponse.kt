package ai.sovereignrag.commons.agent

import java.time.Instant

/**
 * Response from the agent after processing a user message.
 *
 * Contains:
 * - The assistant's reply
 * - Confidence score
 * - Source citations (if RAG is used)
 * - Conversation metadata
 * - Streaming support
 */
data class AgentResponse(
    /**
     * The assistant's reply text
     */
    val message: String,

    /**
     * Unique ID for this response
     */
    val responseId: String,

    /**
     * Session/conversation ID
     */
    val sessionId: String,

    /**
     * Confidence score (0.0 - 1.0)
     * - Below threshold: indicates low confidence, may trigger escalation
     */
    val confidence: Double = 1.0,

    /**
     * Source citations for factual responses (from RAG)
     * Each source includes title, URL, and relevance score
     */
    val sources: List<Source> = emptyList(),

    /**
     * Indicates if this is a streaming chunk (partial response)
     */
    val isStreaming: Boolean = false,

    /**
     * Indicates if this is the final chunk in a streaming response
     */
    val isFinalChunk: Boolean = true,

    /**
     * Timestamp of the response
     */
    val timestamp: Instant = Instant.now(),

    /**
     * Additional metadata (extensibility)
     */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Source citation for RAG-based responses
 */
data class Source(
    /**
     * Title of the source document
     */
    val title: String,

    /**
     * URL or reference to the source
     */
    val url: String,

    /**
     * Relevance score (0.0 - 1.0)
     */
    val relevance: Double = 1.0,

    /**
     * Optional excerpt from the source
     */
    val excerpt: String? = null
)
