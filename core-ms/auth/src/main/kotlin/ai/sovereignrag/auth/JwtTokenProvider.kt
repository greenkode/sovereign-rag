package ai.sovereignrag.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

private val logger = KotlinLogging.logger {}

/**
 * JWT Token Provider for creating and validating JSON Web Tokens
 *
 * Tokens contain the tenant ID as the subject and are signed with HMAC-SHA256
 */
@Component
class JwtTokenProvider(
    @Value("\${sovereignrag.jwt.secret}") private val secretKey: String,
    @Value("\${sovereignrag.jwt.expiration:3600000}") private val validityInMs: Long // 1 hour default
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    /**
     * Create a JWT token for the specified tenant
     *
     * @param tenantId The tenant ID to encode in the token
     * @return JWT token string
     */
    fun createToken(tenantId: String): String {
        val now = Date()
        val validity = Date(now.time + validityInMs)

        logger.debug { "Creating JWT token for tenant: $tenantId (expires in ${validityInMs}ms)" }

        return Jwts.builder()
            .subject(tenantId)
            .issuedAt(now)
            .expiration(validity)
            .signWith(key)
            .compact()
    }

    /**
     * Validate a JWT token
     *
     * @param token The JWT token to validate
     * @return true if valid, false otherwise
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            logger.warn { "Invalid JWT token: ${e.message}" }
            false
        }
    }

    /**
     * Extract tenant ID from JWT token
     *
     * @param token The JWT token
     * @return Tenant ID from token subject
     */
    fun getTenantId(token: String): String {
        val claims: Claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.subject
    }

    /**
     * Get token expiration time in seconds
     *
     * @return Expiration time in seconds
     */
    fun getExpirationTimeInSeconds(): Long {
        return validityInMs / 1000
    }
}
