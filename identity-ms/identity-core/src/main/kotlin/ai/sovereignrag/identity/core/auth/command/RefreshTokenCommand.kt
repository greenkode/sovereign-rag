package ai.sovereignrag.identity.core.auth.command

import an.awesome.pipelinr.Command

data class RefreshTokenCommand(
    val refreshToken: String
) : Command<RefreshTokenResult>

data class RefreshTokenResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val scope: String = "openid email phone profile"
)
