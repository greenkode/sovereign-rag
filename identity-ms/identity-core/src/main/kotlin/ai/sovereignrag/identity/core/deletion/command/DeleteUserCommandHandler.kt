package ai.sovereignrag.identity.core.deletion.command

import ai.sovereignrag.identity.core.deletion.service.UserDeletionService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DeleteUserCommandHandler(
    private val userDeletionService: UserDeletionService
) : Command.Handler<DeleteUserCommand, DeleteUserResult> {

    override fun handle(command: DeleteUserCommand): DeleteUserResult {
        log.info { "Handling delete user command for user: ${command.userId}" }

        val result = userDeletionService.deleteUser(command.userId)

        return DeleteUserResult(
            success = result.success,
            message = result.message,
            userId = result.userId
        )
    }
}
