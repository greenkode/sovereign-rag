package ai.sovereignrag.commons.accounting.dto

import ai.sovereignrag.commons.subscription.SubscriptionTier
import java.time.Instant
import java.util.UUID

data class SubscriptionLimitDto(
    val id: Int,
    val tenantId: UUID,
    val subscriptionTier: SubscriptionTier,
    val dailyTokenLimit: Long,
    val monthlyTokenLimit: Long,
    val start: Instant,
    val expiry: Instant?
)