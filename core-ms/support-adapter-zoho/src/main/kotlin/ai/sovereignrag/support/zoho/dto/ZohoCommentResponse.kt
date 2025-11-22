package ai.sovereignrag.support.zoho.dto

import java.time.Instant

data class ZohoCommentResponse(
    val id: String,
    val content: String,
    val isPublic: Boolean,
    val createdTime: Instant?,
    val authorEmail: String?,
    val authorName: String?
)
