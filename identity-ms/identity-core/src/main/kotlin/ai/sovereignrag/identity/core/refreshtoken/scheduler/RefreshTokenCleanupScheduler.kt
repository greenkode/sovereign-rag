package ai.sovereignrag.identity.core.refreshtoken.scheduler

import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class RefreshTokenCleanupScheduler(
    private val refreshTokenService: RefreshTokenService
) {

    @Scheduled(cron = "0 0 2 * * ?")
    fun cleanupExpiredTokens() {
        log.info { "Starting scheduled cleanup of expired refresh tokens" }
        try {
            val deletedCount = refreshTokenService.cleanupExpiredTokens(daysOld = 30)
            log.info { "Cleanup completed: $deletedCount expired/revoked refresh tokens deleted" }
        } catch (e: Exception) {
            log.error(e) { "Error during refresh token cleanup" }
        }
    }
}
