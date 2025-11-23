package ai.sovereignrag.identity.core.password.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class InitiatePasswordResetCommand(
    val email: String
) : Command<InitiatePasswordResetResult>

data class InitiatePasswordResetResult(
    val reference: UUID,
    val success: Boolean,
    val message: String
)