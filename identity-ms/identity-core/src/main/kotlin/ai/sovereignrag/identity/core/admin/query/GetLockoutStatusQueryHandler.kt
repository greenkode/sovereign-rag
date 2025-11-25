package ai.sovereignrag.identity.core.admin.query

import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.ClientLockoutService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetLockoutStatusQueryHandler(
    private val accountLockoutService: AccountLockoutService,
    private val clientLockoutService: ClientLockoutService
) : Command.Handler<GetLockoutStatusQuery, LockoutStatusResult> {

    override fun handle(query: GetLockoutStatusQuery): LockoutStatusResult {
        val entityType = if (query.isUser) "User" else "Client"
        log.info { "Checking lockout status for $entityType: ${query.identifier}" }

        val remainingMinutes = query.isUser
            .let { isUser ->
                if (isUser) accountLockoutService.getRemainingLockoutMinutes(query.identifier)
                else clientLockoutService.getRemainingLockoutMinutes(query.identifier)
            }

        val isLocked = remainingMinutes != null && remainingMinutes > 0

        return LockoutStatusResult(
            identifier = query.identifier,
            locked = isLocked,
            remainingMinutes = remainingMinutes?.takeIf { it > 0 },
            message = remainingMinutes
                ?.takeIf { it > 0 }
                ?.let { "$entityType is locked for $it more minutes" }
                ?: "$entityType is not locked"
        )
    }
}
