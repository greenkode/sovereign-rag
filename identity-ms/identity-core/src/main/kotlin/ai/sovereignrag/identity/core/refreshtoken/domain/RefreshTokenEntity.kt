package ai.sovereignrag.identity.core.refreshtoken.domain

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_token",
    indexes = [
        Index(name = "idx_refresh_token_jti", columnList = "jti"),
        Index(name = "idx_refresh_token_user_id", columnList = "user_id")
    ]
)
class RefreshTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val jti: String,

    val userId: UUID,

    val tokenHash: String,

    val ipAddress: String? = null,

    val userAgent: String? = null,

    val deviceFingerprint: String? = null,

    val issuedAt: Instant = Instant.now(),

    val expiresAt: Instant,

    var revokedAt: Instant? = null,

    var replacedByJti: String? = null

) : AuditableEntity() {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isRevoked()

    fun revoke(replacedBy: String? = null) {
        this.revokedAt = Instant.now()
        this.replacedByJti = replacedBy
    }
}
