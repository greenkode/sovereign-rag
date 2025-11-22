package ai.sovereignrag.commons.support.dto

import java.time.Instant

data class CommentResult(
    val id: String,
    val content: String,
    val isPublic: Boolean,
    val createdTime: Instant?,
    val authorEmail: String?,
    val authorName: String?
)
