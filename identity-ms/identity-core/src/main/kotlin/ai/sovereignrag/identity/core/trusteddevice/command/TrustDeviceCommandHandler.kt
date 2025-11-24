package ai.sovereignrag.identity.core.trusteddevice.command

import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDevice
import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import ai.sovereignrag.identity.core.trusteddevice.dto.TrustDeviceCommand
import ai.sovereignrag.identity.core.trusteddevice.dto.TrustDeviceResult
import ai.sovereignrag.identity.core.trusteddevice.service.DeviceFingerprintService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Transactional
class TrustDeviceCommandHandler(
    private val trustedDeviceRepository: TrustedDeviceRepository,
    private val deviceFingerprintService: DeviceFingerprintService,
    @Value("\${identity.trusted-device.max-devices-per-user:5}")
    private val maxDevicesPerUser: Int,
    @Value("\${identity.trusted-device.default-duration-days:30}")
    private val defaultDurationDays: Int
) : Command.Handler<TrustDeviceCommand, TrustDeviceResult> {

    override fun handle(command: TrustDeviceCommand): TrustDeviceResult {
        val fingerprintHash = deviceFingerprintService.hashFingerprint(command.deviceFingerprint)
        val trustDuration = Duration.ofDays(command.trustDurationDays.toLong())
        val expiresAt = Instant.now().plus(trustDuration)

        log.info { "Attempting to trust device for user ${command.userId} with session ${command.sessionId}" }

        // Check if device already exists
        val existingDevice = trustedDeviceRepository.findByUserIdAndDeviceFingerprintHashAndExpiresAtAfter(
            command.userId,
            fingerprintHash,
            Instant.now()
        )

        return if (existingDevice != null) {
            // Extend expiration for existing trusted device
            existingDevice.extendExpiration(trustDuration)
            existingDevice.lastUsedAt = Instant.now()

            trustedDeviceRepository.save(existingDevice)

            log.info { "Extended trust for existing device ${existingDevice.id} for user ${command.userId}" }

            TrustDeviceResult(
                deviceId = existingDevice.id!!,
                expiresAt = existingDevice.expiresAt,
                message = "Device trust extended until ${existingDevice.expiresAt}"
            )
        } else {
            // Check device limit
            val currentDeviceCount = trustedDeviceRepository.countByUserIdAndExpiresAtAfter(
                command.userId,
                Instant.now()
            )

            if (currentDeviceCount >= maxDevicesPerUser) {
                // Delete oldest device
                val devices = trustedDeviceRepository.findAllByUserIdAndExpiresAtAfter(
                    command.userId,
                    Instant.now()
                )
                devices.minByOrNull { it.lastUsedAt }?.let {
                    trustedDeviceRepository.delete(it)
                    log.info { "Deleted oldest trusted device ${it.id} to make room for new device" }
                }
            }

            // Create new trusted device
            val trustedDevice = TrustedDevice(
                userId = command.userId,
                deviceFingerprint = command.deviceFingerprint,
                deviceFingerprintHash = fingerprintHash,
                deviceName = command.deviceName ?: deviceFingerprintService.extractDeviceName(command.userAgent),
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                expiresAt = expiresAt
            )

            val savedDevice = trustedDeviceRepository.save(trustedDevice)

            log.info { "Created new trusted device ${savedDevice.id} for user ${command.userId}" }

            TrustDeviceResult(
                deviceId = savedDevice.id!!,
                expiresAt = savedDevice.expiresAt,
                message = "Device trusted until ${savedDevice.expiresAt}"
            )
        }
    }
}