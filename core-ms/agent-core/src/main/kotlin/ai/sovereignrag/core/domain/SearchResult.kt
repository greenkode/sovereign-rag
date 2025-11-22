package ai.sovereignrag.core.domain

import java.time.LocalDateTime

data class SearchResult(
    val uuid: String = "",
    val fact: String,
    val confidence: Double,
    val source: String? = null,
    val validAt: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, String> = emptyMap()
)
