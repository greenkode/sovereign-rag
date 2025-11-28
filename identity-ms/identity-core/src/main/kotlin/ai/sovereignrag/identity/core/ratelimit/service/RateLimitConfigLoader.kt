package ai.sovereignrag.identity.core.ratelimit.service

import ai.sovereignrag.identity.core.ratelimit.domain.RateLimitConfigRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class RateLimitConfigLoader(
    private val rateLimitConfigRepository: RateLimitConfigRepository,
    private val rateLimitConfigService: RateLimitConfigService
) {

    @EventListener(ApplicationReadyEvent::class)
    fun loadRateLimitsIntoCache() {
        log.info { "Loading rate limit configurations into cache..." }

        val configs = rateLimitConfigRepository.findAllByActiveTrue()

        configs.forEach { config ->
            rateLimitConfigService.getConfig(config.methodName, config.subscriptionTier, config.scope)
        }

        log.info { "Loaded ${configs.size} rate limit configurations into cache" }
    }
}
