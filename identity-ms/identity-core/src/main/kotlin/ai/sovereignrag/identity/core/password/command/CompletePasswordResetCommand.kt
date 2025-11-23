package ai.sovereignrag.identity.core.password.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class CompletePasswordResetCommand(
    val reference: String,
    val token: String,
    val newPassword: String
) : Command<CompletePasswordResetResult>

data class CompletePasswordResetResult(
    val success: Boolean,
    val message: String,
    val userId: UUID?
)