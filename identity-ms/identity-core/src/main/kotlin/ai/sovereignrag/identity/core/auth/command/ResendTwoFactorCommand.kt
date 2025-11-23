package ai.sovereignrag.identity.core.auth.command

import an.awesome.pipelinr.Command

data class ResendTwoFactorCommand(
    val sessionId: String,
    val ipAddress: String? = null,
    val userAgent: String? = null
) : Command<ResendTwoFactorResult>

data class ResendTwoFactorResult(
    val sessionId: String,
    val message: String
)