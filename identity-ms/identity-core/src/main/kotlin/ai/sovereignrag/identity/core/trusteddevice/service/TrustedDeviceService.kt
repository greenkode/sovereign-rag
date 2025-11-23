package ai.sovereignrag.identity.core.trusteddevice.service

import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDevice
import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class TrustedDeviceService(
    private val trustedDeviceRepository: TrustedDeviceRepository,
    private val deviceFingerprintService: DeviceFingerprintService
) {

    fun checkTrustedDevice(userId: UUID, deviceFingerprint: String): TrustedDevice? {
        val fingerprintHash = deviceFingerprintService.hashFingerprint(deviceFingerprint)

        log.debug { "Checking trusted device for user $userId with fingerprint hash: $fingerprintHash" }

        val trustedDevice = trustedDeviceRepository.findByUserIdAndDeviceFingerprintHashAndExpiresAtAfter(
            userId,
            fingerprintHash,
            Instant.now()
        )

        if (trustedDevice != null) {
            log.info { "Found trusted device for user $userId, expires at ${trustedDevice.expiresAt}" }

            // Update last used timestamp
            trustedDevice.updateLastUsed()
            trustedDeviceRepository.save(trustedDevice)
        } else {
            log.info { "No trusted device found for user $userId" }
        }

        return trustedDevice
    }
}