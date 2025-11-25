package ai.sovereignrag.identity.core.admin.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.ClientLockoutService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UnlockAccountCommandHandler(
    private val accountLockoutService: AccountLockoutService,
    private val clientLockoutService: ClientLockoutService
) : Command.Handler<UnlockAccountCommand, UnlockAccountResult> {

    override fun handle(command: UnlockAccountCommand): UnlockAccountResult {
        val entityType = if (command.isUser) "user" else "client"
        log.info { "Processing unlock request for $entityType: ${command.identifier}" }

        val unlocked = command.isUser
            .let { isUser ->
                if (isUser) accountLockoutService.unlockAccount(command.identifier)
                else clientLockoutService.unlockClient(command.identifier)
            }

        return unlocked
            .takeIf { it }
            ?.let {
                log.info { "Successfully unlocked $entityType: ${command.identifier}" }
                UnlockAccountResult(
                    status = "success",
                    message = "${entityType.replaceFirstChar { it.uppercase() }} account unlocked successfully",
                    identifier = command.identifier,
                    isUser = command.isUser
                )
            }
            ?: throw NotFoundException("${entityType.replaceFirstChar { it.uppercase() }} not found or was not locked: ${command.identifier}")
    }
}
