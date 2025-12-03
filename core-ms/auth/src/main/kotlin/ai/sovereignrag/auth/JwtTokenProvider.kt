package ai.sovereignrag.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

private val log = KotlinLogging.logger {}

@Component
class JwtTokenProvider(
    @Value("\${sovereignrag.jwt.secret}") private val secretKey: String,
    @Value("\${sovereignrag.jwt.expiration:3600000}") private val validityInMs: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    fun createToken(knowledgeBaseId: String): String {
        val now = Date()
        val validity = Date(now.time + validityInMs)

        log.debug { "Creating JWT token for knowledge base: $knowledgeBaseId (expires in ${validityInMs}ms)" }

        return Jwts.builder()
            .subject(knowledgeBaseId)
            .issuedAt(now)
            .expiration(validity)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return runCatching {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        }.getOrElse { e ->
            log.warn { "Invalid JWT token: ${e.message}" }
            false
        }
    }

    fun getKnowledgeBaseId(token: String): String {
        return getClaims(token).subject
    }

    fun isSetupCompleted(token: String): Boolean {
        return getClaims(token)["setup_completed"] as? Boolean ?: true
    }

    fun getOrganizationStatus(token: String): String? {
        return getClaims(token)["organization_status"] as? String
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getExpirationTimeInSeconds(): Long {
        return validityInMs / 1000
    }
}
