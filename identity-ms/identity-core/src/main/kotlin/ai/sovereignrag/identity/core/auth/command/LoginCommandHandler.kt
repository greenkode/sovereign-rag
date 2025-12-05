package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.FAILED_ATTEMPTS
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.IP_ADDRESS
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.LOCKED_UNTIL
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.LOGIN_METHOD
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.REASON
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USERNAME
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.USER_ID
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.commons.exception.InvalidCredentialsException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.service.AccountLockedException
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.CustomUserDetails
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Profile("local")
class LoginCommandHandler(
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val authenticationManager: AuthenticationManager,
    private val accountLockoutService: AccountLockoutService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService
) : Command.Handler<LoginCommand, LoginResult> {

    override fun handle(command: LoginCommand): LoginResult {
        log.info { "Processing login for user: ${command.username}" }

        return runCatching { authenticate(command) }
            .fold(
                onSuccess = { (userDetails, oauthUser, token) ->
                    accountLockoutService.handleSuccessfulLogin(command.username)
                    publishSuccessfulLoginAudit(command, oauthUser)
                    log.info { "Login successful for user: ${command.username}" }

                    val refreshToken = refreshTokenService.createRefreshToken(
                        user = oauthUser,
                        ipAddress = command.ipAddress,
                        userAgent = null,
                        deviceFingerprint = null
                    )

                    LoginResult(
                        accessToken = token,
                        refreshToken = refreshToken,
                        expiresIn = jwtTokenService.getTokenExpirySeconds(),
                        username = userDetails.username,
                        fullName = userDetails.getFullName(),
                        email = oauthUser.email
                    )
                },
                onFailure = { handleLoginFailure(it, command) }
            )
    }

    private data class AuthenticationResult(
        val userDetails: CustomUserDetails,
        val oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser,
        val token: String
    )

    private fun authenticate(command: LoginCommand): AuthenticationResult {
        val authToken = UsernamePasswordAuthenticationToken(command.username, command.password)
        val authentication = authenticationManager.authenticate(authToken)
        val userDetails = authentication.principal as CustomUserDetails
        val oauthUser = userDetails.getOAuthUser()
        val token = jwtTokenService.generateToken(oauthUser, userDetails)
        return AuthenticationResult(userDetails, oauthUser, token)
    }

    private fun handleLoginFailure(e: Throwable, command: LoginCommand): Nothing {
        when (e) {
            is AccountLockedException -> {
                log.warn { "Account locked for user: ${command.username}, locked until: ${e.lockedUntil}" }
                publishAccountLockedAudit(command, e.failedAttempts, e.lockedUntil)
                throw e
            }
            is LockedException -> {
                log.warn { "Account locked for user: ${command.username}" }
                throw e
            }
            is BadCredentialsException, is UsernameNotFoundException -> {
                log.warn { "Invalid credentials for user: ${command.username}" }
                accountLockoutService.handleFailedLogin(command.username)
                publishFailedLoginAudit(command)
                throw InvalidCredentialsException(messageService.getMessage("auth.error.invalid_credentials"))
            }
            else -> {
                log.error(e) { "Login failed for user: ${command.username}" }
                accountLockoutService.handleFailedLogin(command.username)
                throw InvalidCredentialsException(messageService.getMessage("auth.error.invalid_credentials"))
            }
        }
    }

    private fun publishSuccessfulLoginAudit(
        command: LoginCommand,
        oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser
    ) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = oauthUser.id.toString(),
                actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim()
                    .ifEmpty { oauthUser.email },
                merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login successful - Username: ${command.username}",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    USERNAME.value to command.username,
                    IP_ADDRESS.value to (command.ipAddress ?: "unknown"),
                    USER_ID.value to oauthUser.id.toString(),
                    LOGIN_METHOD.value to "direct_login"
                )
            )
        )
    }

    private fun publishAccountLockedAudit(command: LoginCommand, failedAttempts: Int, lockedUntil: Instant) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = "unknown",
                actorName = command.username,
                merchantId = "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login failed - Account locked - Username: ${command.username}",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    USERNAME.value to command.username,
                    IP_ADDRESS.value to (command.ipAddress ?: "unknown"),
                    REASON.value to "account_locked",
                    FAILED_ATTEMPTS.value to failedAttempts.toString(),
                    LOCKED_UNTIL.value to lockedUntil.toString(),
                    LOGIN_METHOD.value to "direct_login"
                )
            )
        )
    }

    private fun publishFailedLoginAudit(command: LoginCommand) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = "unknown",
                actorName = command.username,
                merchantId = "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login failed - Invalid credentials - Username: ${command.username}",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    USERNAME.value to command.username,
                    IP_ADDRESS.value to (command.ipAddress ?: "unknown"),
                    REASON.value to "invalid_credentials",
                    LOGIN_METHOD.value to "direct_login"
                )
            )
        )
    }
}
