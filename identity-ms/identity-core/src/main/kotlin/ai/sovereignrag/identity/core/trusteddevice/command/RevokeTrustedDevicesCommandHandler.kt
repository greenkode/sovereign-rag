package ai.sovereignrag.identity.core.trusteddevice.command

import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import ai.sovereignrag.identity.core.trusteddevice.dto.RevokeTrustedDeviceCommand
import ai.sovereignrag.identity.core.trusteddevice.dto.RevokeTrustedDeviceResult
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RevokeTrustedDevicesCommandHandler(
    private val trustedDeviceRepository: TrustedDeviceRepository
) : Command.Handler<RevokeTrustedDeviceCommand, RevokeTrustedDeviceResult> {

    override fun handle(command: RevokeTrustedDeviceCommand): RevokeTrustedDeviceResult {
        val revokedCount = command.deviceId
            ?.let { deviceId -> revokeSingleDevice(deviceId, command.userId) }
            ?: revokeAllDevices(command.userId)

        val message = command.deviceId
            ?.let { if (revokedCount > 0) "Device revoked successfully" else "Device not found" }
            ?: if (revokedCount > 0) "$revokedCount trusted devices revoked" else "No trusted devices found"

        log.info { "Revoked $revokedCount trusted devices for user ${command.userId}" }

        return RevokeTrustedDeviceResult(
            devicesRevoked = revokedCount,
            message = message
        )
    }

    private fun revokeSingleDevice(deviceId: java.util.UUID, userId: java.util.UUID): Int {
        log.info { "Deleting trusted device $deviceId for user $userId" }

        return trustedDeviceRepository.findByIdAndUserId(deviceId, userId)
            ?.let { device ->
                trustedDeviceRepository.delete(device)
                1
            }
            ?: 0.also { log.warn { "Device $deviceId not found for user $userId" } }
    }

    private fun revokeAllDevices(userId: java.util.UUID): Int {
        log.info { "Deleting all trusted devices for user $userId" }

        return trustedDeviceRepository.countByUserId(userId).toInt()
            .also { trustedDeviceRepository.deleteAllByUserId(userId) }
    }
}
