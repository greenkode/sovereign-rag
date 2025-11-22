package ai.sovereignrag.auth.api

import mu.KotlinLogging
import ai.sovereignrag.auth.JwtTokenProvider
import ai.sovereignrag.auth.authentication.TenantApiKeyAuthenticationToken
import nl.compilot.ai.commons.tenant.TenantStatus
import nl.compilot.ai.commons.tenant.TenantRegistry
import nl.compilot.ai.commons.dto.AuthRequest
import nl.compilot.ai.commons.dto.AuthResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

/**
 * Authentication controller for JWT-based authentication
 *
 * Uses Spring Security's AuthenticationManager with custom TenantAuthenticationProvider
 * for secure credential validation with BCrypt password encoding
 *
 * Endpoints:
 * - POST /api/auth/authenticate - Get JWT token with tenant credentials
 * - POST /api/auth/refresh - Refresh JWT token (extend session)
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val tenantRegistry: TenantRegistry,
    private val authenticationManager: AuthenticationManager
) {

    /**
     * Authenticate with tenant credentials and receive JWT token
     *
     * This endpoint is called by WordPress server-side (never from browser)
     * to exchange tenant credentials for a JWT token
     *
     * Uses Spring Security's AuthenticationManager which delegates to
     * TenantAuthenticationProvider for BCrypt-based credential validation
     */
    @PostMapping("/authenticate")
    fun authenticate(@RequestBody request: AuthRequest): AuthResponse {
        logger.info { "Authentication request for tenant: ${request.tenantId}" }

        try {
            // Create unauthenticated token with credentials
            val authRequest = TenantApiKeyAuthenticationToken(request.tenantId, request.apiKey)

            // Authenticate using AuthenticationManager
            // This delegates to TenantAuthenticationProvider for BCrypt validation
            val authentication = authenticationManager.authenticate(authRequest)
            val tenantId = authentication.principal as String

            // Verify tenant is active
            val tenant = tenantRegistry.getTenant(tenantId)
            if (tenant.status != TenantStatus.ACTIVE) {
                logger.warn { "Tenant $tenantId is not active: ${tenant.status}" }
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant account is ${tenant.status.name.lowercase()}"
                )
            }

            // Generate JWT token
            val token = jwtTokenProvider.createToken(tenantId)
            val expiresIn = jwtTokenProvider.getExpirationTimeInSeconds()

            logger.info { "JWT token generated for tenant: $tenantId (expires in ${expiresIn}s)" }

            return AuthResponse(
                token = token,
                expiresIn = expiresIn.toInt(),
                tenantId = tenantId
            )
        } catch (e: BadCredentialsException) {
            logger.warn { "Invalid credentials for tenant: ${request.tenantId}" }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        } catch (e: Exception) {
            logger.error(e) { "Authentication failed for tenant: ${request.tenantId}" }
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication failed")
        }
    }

    /**
     * Refresh JWT token for active session
     *
     * This allows extending a user's session without re-authenticating
     * Requires a valid (non-expired) JWT token in Authorization header
     *
     * Called by WordPress plugin before token expires to maintain session
     */
    @PostMapping("/refresh")
    fun refresh(authentication: Authentication): AuthResponse {
        val tenantId = authentication.principal as String
        logger.info { "Token refresh request for tenant: $tenantId" }

        // Verify tenant is still active
        val tenant = try {
            tenantRegistry.getTenant(tenantId)
        } catch (e: Exception) {
            logger.error(e) { "Error fetching tenant: $tenantId" }
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token refresh failed")
        }

        if (tenant == null) {
            logger.warn { "Tenant not found during refresh: $tenantId" }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant not found")
        }

        if (tenant.status != TenantStatus.ACTIVE) {
            logger.warn { "Tenant $tenantId is not active: ${tenant.status}" }
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Tenant account is ${tenant.status.name.lowercase()}"
            )
        }

        // Generate new JWT token
        val token = jwtTokenProvider.createToken(tenant.id)
        val expiresIn = jwtTokenProvider.getExpirationTimeInSeconds()

        logger.info { "JWT token refreshed for tenant: ${tenant.id} (expires in ${expiresIn}s)" }

        return AuthResponse(
            token = token,
            expiresIn = expiresIn.toInt(),
            tenantId = tenant.id
        )
    }
}
