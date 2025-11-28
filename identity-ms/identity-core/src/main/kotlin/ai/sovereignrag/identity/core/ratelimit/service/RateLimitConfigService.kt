package ai.sovereignrag.identity.core.ratelimit.service

import ai.sovereignrag.identity.core.entity.OrganizationPlan
import ai.sovereignrag.identity.core.ratelimit.domain.RateLimitConfig
import ai.sovereignrag.identity.core.ratelimit.domain.RateLimitConfigRepository
import ai.sovereignrag.identity.core.ratelimit.domain.RateLimitScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class RateLimitConfigService(
    private val rateLimitConfigRepository: RateLimitConfigRepository
) {

    fun getConfig(methodName: String, tier: OrganizationPlan, scope: RateLimitScope): RateLimitConfig? {
        log.debug { "Getting rate limit config for method: $methodName, tier: $tier, scope: $scope" }
        return rateLimitConfigRepository.findByMethodNameAndSubscriptionTierAndScopeAndActiveTrue(methodName, tier, scope)
    }

    fun getAllConfigsForMethod(methodName: String, scope: RateLimitScope): List<RateLimitConfig> {
        return rateLimitConfigRepository.findByMethodNameAndScopeAndActiveTrue(methodName, scope)
    }

    fun getAllActiveConfigs(): List<RateLimitConfig> {
        return rateLimitConfigRepository.findAllByActiveTrue()
    }
}
