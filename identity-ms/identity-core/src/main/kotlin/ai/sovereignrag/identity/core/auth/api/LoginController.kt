package ai.sovereignrag.identity.core.auth.api

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.dto.TokenResponse
import ai.sovereignrag.identity.commons.dto.UserSummary
import ai.sovereignrag.identity.commons.exception.InvalidCredentialsException
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.service.AccountLockedException
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.ClientIpExtractionService
import ai.sovereignrag.identity.core.service.CustomUserDetails
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val log = KotlinLogging.logger {}

@RestController
@Profile("local")
class LoginController(
    private val jwtTokenService: JwtTokenService,
    private val authenticationManager: AuthenticationManager,
    private val accountLockoutService: AccountLockoutService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val clientIpExtractionService: ClientIpExtractionService
) {

    @PostMapping("/api/login")
    @RateLimiting(name = "login-attempts", cacheKey = "#loginRequest.username")
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Login successful",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TokenResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ApiResponse(responseCode = "423", description = "Account locked")
    ])
    fun apiLogin(@RequestBody loginRequest: LoginRequest): TokenResponse {
        log.info { "API login attempt for user: ${loginRequest.username}" }

        return runCatching { authenticateUser(loginRequest) }
            .fold(
                onSuccess = { (userDetails, oauthUser, token) ->
                    accountLockoutService.handleSuccessfulLogin(loginRequest.username)
                    publishSuccessfulLoginAudit(loginRequest.username, oauthUser, userDetails)
                    log.info { "Login successful for user: ${loginRequest.username}" }

                    TokenResponse(
                        accessToken = token,
                        expiresIn = jwtTokenService.getTokenExpirySeconds(),
                        user = UserSummary(
                            username = userDetails.username,
                            name = userDetails.getFullName(),
                            email = oauthUser.email
                        )
                    )
                },
                onFailure = { handleLoginException(it, loginRequest.username) }
            )
    }

    private data class AuthResult(
        val userDetails: CustomUserDetails,
        val oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser,
        val token: String
    )

    private fun authenticateUser(loginRequest: LoginRequest): AuthResult {
        val authToken = UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
        val authentication = authenticationManager.authenticate(authToken)
        val userDetails = authentication.principal as CustomUserDetails
        val oauthUser = userDetails.getOAuthUser()
        val token = jwtTokenService.generateToken(oauthUser, userDetails)
        return AuthResult(userDetails, oauthUser, token)
    }

    private fun handleLoginException(e: Throwable, username: String): Nothing {
        when (e) {
            is AccountLockedException -> {
                log.warn { "Account locked for user: $username, locked until: ${e.lockedUntil}" }
                publishAccountLockedAudit(username, e.failedAttempts, e.lockedUntil)
                throw e
            }
            is LockedException -> {
                log.warn { "Account locked for user: $username" }
                throw e
            }
            is BadCredentialsException, is UsernameNotFoundException -> {
                log.warn { "Invalid credentials for user: $username" }
                accountLockoutService.handleFailedLogin(username)
                publishFailedLoginAudit(username, "invalid_credentials")
                throw InvalidCredentialsException()
            }
            else -> {
                log.error(e) { "Login failed for user: $username" }
                accountLockoutService.handleFailedLogin(username)
                throw InvalidCredentialsException()
            }
        }
    }

    private fun publishSuccessfulLoginAudit(
        username: String,
        oauthUser: ai.sovereignrag.identity.core.entity.OAuthUser,
        userDetails: CustomUserDetails
    ) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = oauthUser.id.toString(),
                actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim()
                    .ifEmpty { oauthUser.email },
                merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login successful - Username: $username",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "username" to username,
                    "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                    "userId" to oauthUser.id.toString(),
                    "loginMethod" to "direct_login"
                )
            )
        )
    }

    private fun publishAccountLockedAudit(username: String, failedAttempts: Int, lockedUntil: Instant) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = "unknown",
                actorName = username,
                merchantId = "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login failed - Account locked - Username: $username",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "username" to username,
                    "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                    "reason" to "account_locked",
                    "failedAttempts" to failedAttempts.toString(),
                    "lockedUntil" to lockedUntil.toString(),
                    "loginMethod" to "direct_login"
                )
            )
        )
    }

    private fun publishFailedLoginAudit(username: String, reason: String) {
        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = "unknown",
                actorName = username,
                merchantId = "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "User login failed - Invalid credentials - Username: $username",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "username" to username,
                    "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                    "reason" to reason,
                    "loginMethod" to "direct_login"
                )
            )
        )
    }
}
