package ai.sovereignrag.identity.core.auth.api


import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
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
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@Profile("local")
class LoginController(
    private val jwtTokenService: JwtTokenService,
    private val authenticationManager: AuthenticationManager,
    private val accountLockoutService: AccountLockoutService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val clientIpExtractionService: ClientIpExtractionService
) {

    private val log = KotlinLogging.logger {}

    @PostMapping("/api/login")
    @ResponseBody
    @RateLimiting(
        name = "login-attempts",
        cacheKey = "#loginRequest.username"
    )
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Login successful",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "423", description = "Account locked",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))])
    ])
    fun apiLogin(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        log.info { "API login attempt for user: ${loginRequest.username}" }

        return try {
            // Authenticate user
            val authToken = UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            val authentication = authenticationManager.authenticate(authToken)
            val userDetails = authentication.principal as CustomUserDetails
            val oauthUser = userDetails.getOAuthUser()

            // Handle successful login
            accountLockoutService.handleSuccessfulLogin(loginRequest.username)

            // Generate JWT token using token service
            val token = jwtTokenService.generateToken(oauthUser, userDetails)

            // Publish successful login audit event
            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = oauthUser.id.toString(),
                    actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim()
                        .ifEmpty { oauthUser.email },
                    merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "User login successful - Username: ${loginRequest.username}",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        "username" to loginRequest.username,
                        "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                        "userId" to oauthUser.id.toString(),
                        "loginMethod" to "direct_login"
                    )
                )
            )

            log.info { "Login successful for user: ${loginRequest.username}" }
            ResponseEntity.ok(mapOf(
                "access_token" to token,
                "token_type" to "Bearer",
                "expires_in" to jwtTokenService.getTokenExpirySeconds(),
                "scope" to "openid email phone profile",
                "user" to mapOf(
                    "username" to userDetails.username,
                    "name" to userDetails.getFullName(),
                    "email" to oauthUser.email
                )
            ))
        } catch (e: AccountLockedException) {
            log.warn { "Account locked for user: ${loginRequest.username}, locked until: ${e.lockedUntil}" }
            val remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(loginRequest.username) ?: 0

            // Publish account locked audit event
            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = "unknown",
                    actorName = loginRequest.username,
                    merchantId = "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "User login failed - Account locked - Username: ${loginRequest.username}",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        "username" to loginRequest.username,
                        "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                        "reason" to "account_locked",
                        "failedAttempts" to e.failedAttempts.toString(),
                        "lockedUntil" to e.lockedUntil.toString(),
                        "loginMethod" to "direct_login"
                    )
                )
            )

            ResponseEntity.status(423).body(mapOf(
                "error" to "account_locked",
                "message" to "Account is locked due to ${e.failedAttempts} failed login attempts. Try again in $remainingMinutes minutes.",
                "locked_until" to e.lockedUntil.toString(),
                "remaining_minutes" to remainingMinutes
            ))
        } catch (_: LockedException) {
            log.warn { "Account locked for user: ${loginRequest.username}" }
            val remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(loginRequest.username) ?: 0
            ResponseEntity.status(423).body(mapOf(
                "error" to "account_locked",
                "message" to "Account is locked. Try again in $remainingMinutes minutes.",
                "remaining_minutes" to remainingMinutes
            ))
        } catch (_: BadCredentialsException) {
            log.warn { "Invalid credentials for user: ${loginRequest.username}" }
            accountLockoutService.handleFailedLogin(loginRequest.username)

            // Publish failed login audit event
            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = "unknown",
                    actorName = loginRequest.username,
                    merchantId = "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "User login failed - Invalid credentials - Username: ${loginRequest.username}",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        "username" to loginRequest.username,
                        "ipAddress" to clientIpExtractionService.getClientIpAddressFromContext(),
                        "reason" to "invalid_credentials",
                        "loginMethod" to "direct_login"
                    )
                )
            )

            ResponseEntity.status(401).body(mapOf(
                "error" to "invalid_credentials",
                "message" to "Invalid username or password"
            ))
        } catch (_: UsernameNotFoundException) {
            log.warn { "User not found: ${loginRequest.username}" }
            ResponseEntity.status(401).body(mapOf(
                "error" to "invalid_credentials",
                "message" to "Invalid username or password"
            ))
        } catch (e: Exception) {
            log.error(e) { "Login failed for user: ${loginRequest.username}" }
            accountLockoutService.handleFailedLogin(loginRequest.username)
            ResponseEntity.status(401).body(mapOf(
                "error" to "invalid_credentials",
                "message" to "Invalid username or password"
            ))
        }
    }
}