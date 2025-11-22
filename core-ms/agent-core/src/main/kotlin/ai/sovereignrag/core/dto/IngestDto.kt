package ai.sovereignrag.core.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class IngestRequest(
    val title: String,
    val content: String,
    val url: String,
    @JsonProperty("post_type")
    val postType: String = "post",
    val date: OffsetDateTime? = null
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
