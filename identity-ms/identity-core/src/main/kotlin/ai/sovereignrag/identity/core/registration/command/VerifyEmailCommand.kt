package ai.sovereignrag.identity.core.registration.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class VerifyEmailCommand(
    val token: String
) : Command<VerifyEmailResult>

data class VerifyEmailResult(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null
)
