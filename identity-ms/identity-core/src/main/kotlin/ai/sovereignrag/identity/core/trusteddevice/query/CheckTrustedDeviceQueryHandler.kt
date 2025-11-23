package ai.sovereignrag.identity.core.trusteddevice.query

import ai.sovereignrag.identity.core.trusteddevice.dto.CheckTrustedDeviceQuery
import ai.sovereignrag.identity.core.trusteddevice.dto.CheckTrustedDeviceResult
import ai.sovereignrag.identity.core.trusteddevice.service.TrustedDeviceService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class CheckTrustedDeviceQueryHandler(
    private val trustedDeviceService: TrustedDeviceService
) : Command.Handler<CheckTrustedDeviceQuery, CheckTrustedDeviceResult> {

    override fun handle(query: CheckTrustedDeviceQuery): CheckTrustedDeviceResult {
        val trustedDevice = trustedDeviceService.checkTrustedDevice(query.userId, query.deviceFingerprint)

        return if (trustedDevice != null) {
            CheckTrustedDeviceResult(
                isTrusted = true,
                deviceId = trustedDevice.id,
                lastUsedAt = trustedDevice.lastUsedAt,
                expiresAt = trustedDevice.expiresAt
            )
        } else {
            CheckTrustedDeviceResult(isTrusted = false)
        }
    }
}