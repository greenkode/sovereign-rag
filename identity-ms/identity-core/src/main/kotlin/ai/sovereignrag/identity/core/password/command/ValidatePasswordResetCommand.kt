package ai.sovereignrag.identity.core.password.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class ValidatePasswordResetCommand(
    val token: String
) : Command<ValidatePasswordResetResult>

data class ValidatePasswordResetResult(
    val success: Boolean,
    val message: String,
    val reference: UUID,
    val userId: UUID
)