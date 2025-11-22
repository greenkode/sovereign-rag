package ai.sovereignrag.support.zoho.dto

data class ZohoCommentRequest(
    val content: String,
    val isPublic: Boolean = true
)
