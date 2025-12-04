package ai.sovereignrag.identity.core.deletion.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class DeleteUserCommand(
    val userId: UUID
) : Command<DeleteUserResult>

data class DeleteUserResult(
    val success: Boolean,
    val message: String,
    val userId: UUID
)
