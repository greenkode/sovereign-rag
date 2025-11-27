package ai.sovereignrag.identity.core.registration.command

import an.awesome.pipelinr.Command

data class ResendVerificationCommand(
    val email: String
) : Command<ResendVerificationResult>

data class ResendVerificationResult(
    val success: Boolean,
    val message: String
)
