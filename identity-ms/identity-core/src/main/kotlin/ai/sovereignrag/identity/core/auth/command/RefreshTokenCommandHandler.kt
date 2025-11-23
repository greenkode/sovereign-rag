package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CustomUserDetails
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RefreshTokenCommandHandler(
    private val jwtDecoder: JwtDecoder,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: OAuthUserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<RefreshTokenCommand, RefreshTokenResult> {

    override fun handle(command: RefreshTokenCommand): RefreshTokenResult {
        log.info { "Refresh token request received" }

        try {
            val jwt = jwtDecoder.decode(command.refreshToken)
            val userId = jwt.subject
            val jti = jwt.claims["jti"] as? String
            val tokenType = jwt.claims["token_type"] as? String

            if (userId.isNullOrBlank() || jti.isNullOrBlank()) {
                throw ClientException("Invalid refresh token")
            }

            if (tokenType != "refresh") {
                throw ClientException("Token is not a refresh token")
            }

            val storedToken = refreshTokenService.validateAndGetRefreshToken(command.refreshToken, jti)

            val user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow { ClientException("User not found") }

            if (!user.emailVerified) {
                throw ClientException("Email not verified")
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
                        "username" to user.username,
                        "oldJti" to jti,
                        "userId" to user.id.toString()
                    )
                )
            )

            log.info { "Token refresh successful for user: ${user.username}" }

            return RefreshTokenResult(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = jwtTokenService.getTokenExpirySeconds()
            )
        } catch (e: ClientException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Error during token refresh" }
            throw ClientException("Invalid or expired refresh token")
        }
    }
}
