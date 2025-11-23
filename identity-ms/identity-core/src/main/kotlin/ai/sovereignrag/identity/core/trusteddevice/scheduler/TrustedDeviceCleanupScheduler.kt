package ai.sovereignrag.identity.core.trusteddevice.scheduler

import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(
    name = ["identity.trusted-device.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TrustedDeviceCleanupScheduler(
    private val trustedDeviceRepository: TrustedDeviceRepository
) {

    @Scheduled(cron = "\${identity.trusted-device.cleanup-expired-cron:0 0 2 * * *}")
    @Transactional
    fun cleanupExpiredDevices() {
        log.info { "Starting cleanup of expired trusted devices" }

        val now = Instant.now()
        try {
            trustedDeviceRepository.deleteExpiredDevices(now)
            trustedDeviceRepository.flush()

            log.info { "Cleanup of expired trusted devices completed successfully" }
        } catch (e: Exception) {
            log.error(e) { "Failed to cleanup expired trusted devices" }
            throw e
        }

        log.info { "Expired trusted devices cleanup completed" }
    }
}