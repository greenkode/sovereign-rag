package ai.sovereignrag.core.domain

import java.time.LocalDateTime

/**
 * Simplified content document for LangChain4j RAG
 */
data class ContentDocument(
    val id: String,
    val title: String,
    val content: String,
    val url: String? = null,
    val source: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, String> = emptyMap()
)
