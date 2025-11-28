package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class LogoutCommandHandler(
    private val refreshTokenService: RefreshTokenService,
    private val messageService: MessageService
) : Command.Handler<LogoutCommand, LogoutResult> {

    override fun handle(command: LogoutCommand): LogoutResult {
        log.info { "Logout request received" }

        return runCatching {
            refreshTokenService.revokeRefreshTokenByHash(command.refreshToken)
            log.info { "Logout successful - refresh token revoked" }

            LogoutResult(
                success = true,
                message = messageService.getMessage("auth.success.logout")
            )
        }.getOrElse { e ->
            log.warn(e) { "Error during logout" }
            LogoutResult(
                success = true,
                message = messageService.getMessage("auth.success.logout")
            )
        }
    }
}
