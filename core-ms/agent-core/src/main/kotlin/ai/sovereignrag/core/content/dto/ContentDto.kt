package ai.sovereignrag.core.content.dto

import com.fasterxml.jackson.annotation.JsonProperty
import nl.compilot.ai.domain.SearchResult
import java.time.OffsetDateTime

// Ingest DTOs
data class IngestRequest(
    val title: String,
    val content: String,
    val url: String,
    @JsonProperty("post_type")
    val postType: String = "post",
    val date: OffsetDateTime? = null,

    // Contextual metadata for enhanced RAG
    @JsonProperty("site_title")
    val siteTitle: String? = null,
    @JsonProperty("site_tagline")
    val siteTagline: String? = null,
    val category: String? = null,
    @JsonProperty("category_description")
    val categoryDescription: String? = null,
    val tags: List<String>? = null,
    val excerpt: String? = null,
    val author: String? = null,
    @JsonProperty("author_bio")
    val authorBio: String? = null,
    @JsonProperty("related_posts")
    val relatedPosts: List<String>? = null,
    val breadcrumb: String? = null
)

data class IngestResponse(
    val status: String,
    @JsonProperty("task_id")
    val taskId: String? = null,
    val message: String? = null
)

data class IngestStatusResponse(
    val status: String,
    val progress: String? = null,
    val error: String? = null
)

// Search DTOs
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

// Delete DTOs
data class DeleteDocumentResponse(
    val success: Boolean,
    val message: String
)
