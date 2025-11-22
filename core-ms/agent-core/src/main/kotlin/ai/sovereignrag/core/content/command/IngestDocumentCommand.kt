package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import ai.sovereignrag.content.dto.IngestResponse
import java.time.OffsetDateTime

data class IngestDocumentCommand(
    val title: String,
    val content: String,
    val url: String,
    val postType: String = "post",
    val date: OffsetDateTime? = null,

    // Contextual metadata
    val siteTitle: String? = null,
    val siteTagline: String? = null,
    val category: String? = null,
    val categoryDescription: String? = null,
    val tags: List<String>? = null,
    val excerpt: String? = null,
    val author: String? = null,
    val authorBio: String? = null,
    val relatedPosts: List<String>? = null,
    val breadcrumb: String? = null
) : Command<IngestResponse>
