package ai.sovereignrag.identity.core.refreshtoken.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    schema = "identity",
    indexes = [
        Index(name = "idx_refresh_token_jti", columnList = "jti"),
        Index(name = "idx_refresh_token_user_id", columnList = "user_id")
    ]
)
class RefreshTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val jti: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 500)
    val userAgent: String? = null,

    @Column(name = "device_fingerprint", length = 255)
    val deviceFingerprint: String? = null,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "replaced_by_jti")
    var replacedByJti: String? = null,

    val createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now()

) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isRevoked()

    fun revoke(replacedBy: String? = null) {
        this.revokedAt = Instant.now()
        this.replacedByJti = replacedBy
        this.updatedAt = Instant.now()
    }
}
