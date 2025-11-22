package nl.compilot.ai.support.zoho.dto

data class ZohoContactRequest(
    val email: String,
    val firstName: String?,
    val lastName: String?
)
