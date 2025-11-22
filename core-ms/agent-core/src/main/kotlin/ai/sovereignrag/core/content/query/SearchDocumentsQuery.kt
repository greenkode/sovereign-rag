package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import ai.sovereignrag.content.dto.SearchResponse

data class SearchDocumentsQuery(
    val query: String,
    val numResults: Int = 10,
    val minConfidence: Double,
    val lowConfidenceThreshold: Double? = null,
    val returnMode: String = "multiple"
) : Command<SearchResponse>
