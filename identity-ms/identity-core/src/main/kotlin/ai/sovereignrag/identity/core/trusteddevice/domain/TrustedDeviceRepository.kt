package ai.sovereignrag.identity.core.trusteddevice.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface TrustedDeviceRepository : JpaRepository<TrustedDevice, UUID> {

    fun findByUserIdAndDeviceFingerprintHashAndExpiresAtAfter(
        userId: UUID,
        deviceFingerprintHash: String,
        expiresAt: Instant
    ): TrustedDevice?

    fun findAllByUserId(userId: UUID): List<TrustedDevice>

    fun findAllByUserIdAndExpiresAtAfter(userId: UUID, expiresAt: Instant): List<TrustedDevice>

    @Modifying
    @Query("DELETE FROM TrustedDevice td WHERE td.userId = :userId")
    fun deleteAllByUserId(userId: UUID)

    @Modifying
    @Query("DELETE FROM TrustedDevice td WHERE td.expiresAt < :now")
    fun deleteExpiredDevices(now: Instant)

    fun countByUserIdAndExpiresAtAfter(userId: UUID, expiresAt: Instant): Long

    fun countByUserId(userId: UUID): Long

    fun findByIdAndUserId(deviceId: UUID, userId: UUID): TrustedDevice?
}