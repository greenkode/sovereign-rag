package ai.sovereignrag.commons.dto

/**
 * Authentication request DTO
 * Used to authenticate with tenant credentials
 */
data class AuthRequest(
    val tenantId: String,
    val apiKey: String
)

/**
 * Authentication response DTO
 * Contains JWT token and expiration info
 */
data class AuthResponse(
    val token: String,
    val expiresIn: Int,
    val tenantId: String
)
