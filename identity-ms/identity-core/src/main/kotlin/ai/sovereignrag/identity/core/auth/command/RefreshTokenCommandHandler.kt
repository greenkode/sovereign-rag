package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.OLD_JTI
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USERNAME
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USER_ID
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CustomUserDetails
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RefreshTokenCommandHandler(
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: OAuthUserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService
) : Command.Handler<RefreshTokenCommand, RefreshTokenResult> {

    override fun handle(command: RefreshTokenCommand): RefreshTokenResult {
        log.info { "Refresh token request received" }

        return runCatching {
            val storedToken = refreshTokenService.validateAndGetRefreshTokenByHash(command.refreshToken)

            val user = userRepository.findById(storedToken.userId)
                .orElseThrow { ClientException(messageService.getMessage("auth.error.user_not_found")) }

            if (!user.emailVerified) {
                throw ClientException(messageService.getMessage("auth.error.email_not_verified"))
            }

            val userDetails = CustomUserDetails(user)
            val newAccessToken = jwtTokenService.generateToken(user, userDetails)
            val newRefreshToken = refreshTokenService.rotateRefreshToken(storedToken, user)

            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = user.id.toString(),
                    actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                    merchantId = user.merchantId?.toString() ?: "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "Access token refreshed",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        USERNAME.value to user.username,
                        OLD_JTI.value to storedToken.jti,
                        USER_ID.value to user.id.toString()
                    )
                )
            )

            log.info { "Token refresh successful for user: ${user.username}" }

            RefreshTokenResult(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = jwtTokenService.getTokenExpirySeconds()
            )
        }.getOrElse { e ->
            when (e) {
                is ClientException -> throw e
                else -> {
                    log.error(e) { "Error during token refresh" }
                    throw ClientException(messageService.getMessage("auth.error.invalid_or_expired_refresh_token"))
                }
            }
        }
    }
}
