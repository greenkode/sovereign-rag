package ai.sovereignrag.identity.core.trusteddevice.service

import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDevice
import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class TrustedDeviceService(
    private val trustedDeviceRepository: TrustedDeviceRepository,
    private val deviceFingerprintService: DeviceFingerprintService
) {

    fun checkTrustedDevice(userId: UUID, deviceFingerprint: String): TrustedDevice? =
        deviceFingerprintService.hashFingerprint(deviceFingerprint)
            .also { log.debug { "Checking trusted device for user $userId with fingerprint hash: $it" } }
            .let { fingerprintHash ->
                trustedDeviceRepository.findByUserIdAndDeviceFingerprintHashAndExpiresAtAfter(
                    userId,
                    fingerprintHash,
                    Instant.now()
                )
            }
            ?.also { device ->
                log.info { "Found trusted device for user $userId, expires at ${device.expiresAt}" }
                device.updateLastUsed()
                trustedDeviceRepository.save(device)
            }
            ?: run {
                log.info { "No trusted device found for user $userId" }
                null
            }
}