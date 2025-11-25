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

        return trustedDeviceRepository.findByUserIdAndDeviceFingerprintHashAndExpiresAtAfter(
            command.userId,
            fingerprintHash,
            Instant.now()
        )?.let { existingDevice ->
            extendExistingDeviceTrust(existingDevice, trustDuration, command.userId)
        } ?: createNewTrustedDevice(command, fingerprintHash, expiresAt)
    }

    private fun extendExistingDeviceTrust(
        device: TrustedDevice,
        trustDuration: Duration,
        userId: java.util.UUID
    ): TrustDeviceResult {
        device.extendExpiration(trustDuration)
        device.lastUsedAt = Instant.now()
        trustedDeviceRepository.save(device)

        log.info { "Extended trust for existing device ${device.id} for user $userId" }

        return TrustDeviceResult(
            deviceId = device.id!!,
            expiresAt = device.expiresAt,
            message = "Device trust extended until ${device.expiresAt}"
        )
    }

    private fun createNewTrustedDevice(
        command: TrustDeviceCommand,
        fingerprintHash: String,
        expiresAt: Instant
    ): TrustDeviceResult {
        enforceDeviceLimit(command.userId)

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

        return TrustDeviceResult(
            deviceId = savedDevice.id!!,
            expiresAt = savedDevice.expiresAt,
            message = "Device trusted until ${savedDevice.expiresAt}"
        )
    }

    private fun enforceDeviceLimit(userId: java.util.UUID) {
        val currentDeviceCount = trustedDeviceRepository.countByUserIdAndExpiresAtAfter(userId, Instant.now())

        currentDeviceCount.takeIf { it >= maxDevicesPerUser }?.let {
            trustedDeviceRepository.findAllByUserIdAndExpiresAtAfter(userId, Instant.now())
                .minByOrNull { device -> device.lastUsedAt }
                ?.let { oldestDevice ->
                    trustedDeviceRepository.delete(oldestDevice)
                    log.info { "Deleted oldest trusted device ${oldestDevice.id} to make room for new device" }
                }
        }
    }
}
