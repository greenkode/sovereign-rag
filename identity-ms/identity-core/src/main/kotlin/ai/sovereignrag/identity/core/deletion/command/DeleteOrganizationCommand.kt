package ai.sovereignrag.identity.core.deletion.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class DeleteOrganizationCommand(
    val organizationId: UUID
) : Command<DeleteOrganizationResult>

data class DeleteOrganizationResult(
    val success: Boolean,
    val message: String,
    val organizationId: UUID,
    val usersDeleted: Int
)
