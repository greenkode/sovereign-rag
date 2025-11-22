package ai.sovereignrag.tenant.domain

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.*

/**
 * Reset Token JPA entity for secure API key reset flow
 *
 * Tokens are:
 * - Single-use only (usedAt timestamp)
 * - Time-limited (15 minutes expiry)
 * - BCrypt hashed for security
 * - Tied to specific tenant
 */
@Entity
@Table(name = "reset_tokens", schema = "master")
data class ResetToken(
    @Id
    @Column(name = "id", nullable = false, length = 255)
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "tenant_id", nullable = false, length = 255)
    val tenantId: String,

    @Column(name = "token_hash", nullable = false, length = 255)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        /**
         * Token validity period in minutes
         */
        const val VALIDITY_MINUTES = 15L
    }

    /**
     * Check if token is still valid (not expired and not used)
     */
    fun isValid(): Boolean {
        return usedAt == null && Instant.now().isBefore(expiresAt)
    }

    /**
     * Check if token is expired
     */
    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }

    /**
     * Mark token as used
     */
    fun markAsUsed() {
        usedAt = Instant.now()
    }
}
