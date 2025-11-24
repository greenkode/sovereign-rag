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
        val revokedCount = if (command.deviceId != null) {
            log.info { "Deleting trusted device ${command.deviceId} for user ${command.userId}" }
            val device = trustedDeviceRepository.findByIdAndUserId(command.deviceId, command.userId)
            if (device != null) {
                trustedDeviceRepository.delete(device)
                1
            } else {
                log.warn { "Device ${command.deviceId} not found for user ${command.userId}" }
                0
            }
        } else {
            log.info { "Deleting all trusted devices for user ${command.userId}" }
            val count = trustedDeviceRepository.countByUserId(command.userId)
            trustedDeviceRepository.deleteAllByUserId(command.userId)
            count.toInt()
        }

        val message = if (command.deviceId != null) {
            if (revokedCount > 0) "Device revoked successfully" else "Device not found"
        } else {
            if (revokedCount > 0) "$revokedCount trusted devices revoked" else "No trusted devices found"
        }

        log.info { "Revoked $revokedCount trusted devices for user ${command.userId}" }

        return RevokeTrustedDeviceResult(
            devicesRevoked = revokedCount,
            message = message
        )
    }
}