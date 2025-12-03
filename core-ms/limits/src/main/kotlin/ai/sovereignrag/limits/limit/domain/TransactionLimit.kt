package ai.sovereignrag.limits.limit.domain

import ai.sovereignrag.commons.subscription.SubscriptionTier
import java.time.Instant
import java.util.UUID

data class SubscriptionLimit(
    val id: Int,
    val organizationId: UUID,
    val subscriptionTier: SubscriptionTier,
    val dailyTokenLimit: Long,
    val monthlyTokenLimit: Long,
    val start: Instant,
    val expiry: Instant?
)