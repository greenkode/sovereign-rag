package ai.sovereignrag.identity.core.auth.command

import an.awesome.pipelinr.Command

data class LoginCommand(
    val username: String,
    val password: String,
    val ipAddress: String? = null
) : Command<LoginResult>

data class LoginResult(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val username: String,
    val fullName: String,
    val email: String
)
