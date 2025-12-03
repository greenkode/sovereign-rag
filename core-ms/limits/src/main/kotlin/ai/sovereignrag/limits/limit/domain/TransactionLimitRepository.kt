package ai.sovereignrag.limits.limit.domain

import ai.sovereignrag.commons.cache.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface SubscriptionLimitRepository : JpaRepository<SubscriptionLimitEntity, Int> {

    @Cacheable(value = [CacheNames.ACCOUNT], key = "'subscription_limit_' + #organizationId.toString()", unless = "#result == null")
    fun findByOrganizationIdAndStartBeforeAndExpiryIsNullOrExpiryAfter(
        organizationId: UUID,
        start: Instant,
        end: Instant
    ): SubscriptionLimitEntity?

    @CacheEvict(value = [CacheNames.ACCOUNT], allEntries = true)
    override fun <S : SubscriptionLimitEntity> save(entity: S): S

    @CacheEvict(value = [CacheNames.ACCOUNT], allEntries = true)
    override fun <S : SubscriptionLimitEntity> saveAll(entities: MutableIterable<S>): MutableList<S>
}