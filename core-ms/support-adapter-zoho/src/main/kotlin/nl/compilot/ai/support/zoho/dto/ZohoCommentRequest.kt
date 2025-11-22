package nl.compilot.ai.support.zoho.dto

data class ZohoCommentRequest(
    val content: String,
    val isPublic: Boolean = true
)
