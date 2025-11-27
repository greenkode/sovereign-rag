package ai.sovereignrag.identity.core.trusteddevice.domain

import ai.sovereignrag.identity.commons.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trusted_devices", schema = "identity")
class TrustedDevice(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val userId: UUID,

    val deviceFingerprint: String,

    val deviceFingerprintHash: String,

    val deviceName: String? = null,

    val ipAddress: String? = null,

    val userAgent: String? = null,

    val trustedAt: Instant = Instant.now(),

    var expiresAt: Instant,

    var lastUsedAt: Instant = Instant.now(),

    var trustCount: Int = 1

) : AuditableEntity() {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun updateLastUsed() {
        this.lastUsedAt = Instant.now()
    }

    fun extendExpiration(duration: java.time.Duration) {
        this.expiresAt = Instant.now().plus(duration)
        this.trustCount++
    }
}
