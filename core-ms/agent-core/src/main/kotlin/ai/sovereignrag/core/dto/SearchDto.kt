package ai.sovereignrag.core.dto

import com.fasterxml.jackson.annotation.JsonProperty
import ai.sovereignrag.domain.SearchResult

data class SearchRequest(
    val query: String,
    @JsonProperty("num_results")
    val numResults: Int = 10,
    @JsonProperty("min_confidence")
    val minConfidence: Double,
    @JsonProperty("low_confidence_threshold")
    val lowConfidenceThreshold: Double? = null,
    @JsonProperty("return_mode")
    val returnMode: String = "multiple" // "single" or "multiple"
)

data class SearchResponse(
    val results: List<SearchResult>,
    val flagged: Boolean = false,
    @JsonProperty("flag_reason")
    val flagReason: String? = null,
    val threshold: Double? = null
)

data class AutocompleteResponse(
    val suggestions: List<String>
)
