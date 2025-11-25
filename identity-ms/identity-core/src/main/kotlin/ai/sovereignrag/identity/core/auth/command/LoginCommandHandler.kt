package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.InvalidCredentialsException
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
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
    private val authenticationManager: AuthenticationManager,
    private val accountLockoutService: AccountLockoutService,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<LoginCommand, LoginResult> {

    override fun handle(command: LoginCommand): LoginResult {
        log.info { "Processing login for user: ${command.username}" }

        return runCatching { authenticate(command) }
            .fold(
                onSuccess = { (userDetails, oauthUser, token) ->
                    accountLockoutService.handleSuccessfulLogin(command.username)
                    publishSuccessfulLoginAudit(command, oauthUser)
                    log.info { "Login successful for user: ${command.username}" }

                    LoginResult(
                        accessToken = token,
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
                throw InvalidCredentialsException()
            }
            else -> {
                log.error(e) { "Login failed for user: ${command.username}" }
                accountLockoutService.handleFailedLogin(command.username)
                throw InvalidCredentialsException()
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
                    "username" to command.username,
                    "ipAddress" to (command.ipAddress ?: "unknown"),
                    "userId" to oauthUser.id.toString(),
                    "loginMethod" to "direct_login"
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
                    "username" to command.username,
                    "ipAddress" to (command.ipAddress ?: "unknown"),
                    "reason" to "account_locked",
                    "failedAttempts" to failedAttempts.toString(),
                    "lockedUntil" to lockedUntil.toString(),
                    "loginMethod" to "direct_login"
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
                    "username" to command.username,
                    "ipAddress" to (command.ipAddress ?: "unknown"),
                    "reason" to "invalid_credentials",
                    "loginMethod" to "direct_login"
                )
            )
        )
    }
}
