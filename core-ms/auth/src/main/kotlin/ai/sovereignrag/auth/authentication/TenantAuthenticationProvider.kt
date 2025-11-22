package ai.sovereignrag.auth.authentication

import mu.KotlinLogging
import nl.compilot.ai.commons.tenant.TenantRegistry
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Custom authentication provider for tenant API key authentication
 *
 * This provider validates tenant credentials using:
 * - TenantRegistry for tenant lookup
 * - DelegatingPasswordEncoder for secure password/API key verification
 *
 * Security features:
 * - Uses DelegatingPasswordEncoder which auto-detects algorithm from hash prefix (e.g., {bcrypt}$2a$10...)
 * - BCrypt provides built-in salt and adaptive work factor
 * - Constant-time comparison to prevent timing attacks
 * - Comprehensive logging for security auditing
 */
@Component
class TenantAuthenticationProvider(
    private val tenantRegistry: TenantRegistry,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationProvider {

    /**
     * Authenticates a tenant using their API key
     *
     * Uses DelegatingPasswordEncoder which automatically detects the hash algorithm
     * from the prefix in the stored hash (e.g., {bcrypt}$2a$10...)
     *
     * @param authentication The authentication request (must be TenantApiKeyAuthenticationToken)
     * @return Authenticated token with tenant ID as principal
     * @throws BadCredentialsException if credentials are invalid
     */
    override fun authenticate(authentication: Authentication): Authentication {
        require(authentication is TenantApiKeyAuthenticationToken) {
            "Unsupported authentication type: ${authentication.javaClass.name}"
        }

        val tenantId = authentication.getTenantId()
        val apiKey = authentication.credentials as? String
            ?: throw BadCredentialsException("API key is required")

        logger.debug { "Authenticating tenant: $tenantId" }

        try {
            // Look up tenant from registry
            val tenant = tenantRegistry.getTenant(tenantId)

            // Validate API key using DelegatingPasswordEncoder
            // The encoder automatically detects the algorithm from the hash prefix
            val isValid = passwordEncoder.matches(apiKey, tenant.apiKeyHash)

            if (!isValid) {
                logger.warn { "Invalid API key for tenant: $tenantId" }
                throw BadCredentialsException("Invalid API key for tenant: $tenantId")
            }

            logger.info { "Successfully authenticated tenant: $tenantId" }

            // Create authenticated token with ROLE_TENANT authority
            val authorities = listOf(SimpleGrantedAuthority("ROLE_TENANT"))
            return TenantApiKeyAuthenticationToken(tenantId, authorities)

        } catch (e: Exception) {
            when (e) {
                is BadCredentialsException -> throw e
                else -> {
                    logger.error(e) { "Authentication failed for tenant: $tenantId" }
                    throw BadCredentialsException("Authentication failed for tenant: $tenantId", e)
                }
            }
        }
    }

    /**
     * Indicates whether this provider supports the given authentication type
     */
    override fun supports(authentication: Class<*>): Boolean {
        return TenantApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}
