package ai.sovereignrag.commons.dto

data class AuthRequest(
    val knowledgeBaseId: String,
    val apiKey: String
)

data class AuthResponse(
    val token: String,
    val expiresIn: Int,
    val knowledgeBaseId: String
)
