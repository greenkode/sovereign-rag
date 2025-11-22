package ai.sovereignrag.commons.support.dto

data class AddCommentRequest(
    val content: String,
    val isPublic: Boolean = true
)
