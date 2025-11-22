package nl.compilot.ai.support.zoho.dto

data class ZohoContactResponse(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?
)
