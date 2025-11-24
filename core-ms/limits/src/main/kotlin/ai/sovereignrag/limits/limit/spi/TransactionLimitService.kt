package ai.sovereignrag.limits.limit.spi

import ai.sovereignrag.limits.limit.domain.SubscriptionLimit
import ai.sovereignrag.limits.limit.domain.SubscriptionLimitRepository
import ai.sovereignrag.commons.accounting.SubscriptionLimitGateway
import ai.sovereignrag.commons.accounting.dto.SubscriptionLimitDto
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SubscriptionLimitService(private val subscriptionLimitRepository: SubscriptionLimitRepository) :
    SubscriptionLimitGateway {

    override fun findByTenant(tenantId: UUID): SubscriptionLimitDto? {
        return subscriptionLimitRepository.findByTenantIdAndStartBeforeAndExpiryIsNullOrExpiryAfter(
            tenantId,
            Instant.now(),
            Instant.now()
        )?.toDomain()
            ?.let {
                SubscriptionLimitDto(
                    it.id,
                    it.tenantId,
                    it.subscriptionTier,
                    it.dailyTokenLimit,
                    it.monthlyTokenLimit,
                    it.start,
                    it.expiry
                )
            }
    }
}