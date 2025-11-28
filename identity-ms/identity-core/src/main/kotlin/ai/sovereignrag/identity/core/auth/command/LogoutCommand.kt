package ai.sovereignrag.identity.core.auth.command

import an.awesome.pipelinr.Command

data class LogoutCommand(
    val refreshToken: String
) : Command<LogoutResult>

data class LogoutResult(
    val success: Boolean,
    val message: String
)
