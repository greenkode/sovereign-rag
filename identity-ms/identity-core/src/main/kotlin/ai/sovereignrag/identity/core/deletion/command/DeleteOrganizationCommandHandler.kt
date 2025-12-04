package ai.sovereignrag.identity.core.deletion.command

import ai.sovereignrag.identity.core.deletion.service.OrganizationDeletionService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DeleteOrganizationCommandHandler(
    private val organizationDeletionService: OrganizationDeletionService
) : Command.Handler<DeleteOrganizationCommand, DeleteOrganizationResult> {

    override fun handle(command: DeleteOrganizationCommand): DeleteOrganizationResult {
        log.info { "Handling delete organization command for organization: ${command.organizationId}" }

        val result = organizationDeletionService.deleteOrganization(command.organizationId)

        return DeleteOrganizationResult(
            success = result.success,
            message = result.message,
            organizationId = result.organizationId,
            usersDeleted = result.usersDeleted
        )
    }
}
