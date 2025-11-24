package ai.sovereignrag.identity.core.trusteddevice.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trusted_devices", schema = "identity")
class TrustedDevice(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "device_fingerprint", nullable = false)
    val deviceFingerprint: String,

    @Column(name = "device_fingerprint_hash", nullable = false)
    val deviceFingerprintHash: String,

    @Column(name = "device_name")
    val deviceName: String? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null,

    @Column(name = "user_agent")
    val userAgent: String? = null,

    @Column(name = "trusted_at", nullable = false)
    val trustedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant = Instant.now(),

    @Column(name = "trust_count")
    var trustCount: Int = 1,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),

) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun updateLastUsed() {
        this.lastUsedAt = Instant.now()
    }

    fun extendExpiration(duration: java.time.Duration) {
        this.expiresAt = Instant.now().plus(duration)
        this.trustCount++
    }
}