package ai.sovereignrag.identity.core.ratelimit.domain

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.subscription.SubscriptionTier
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
class RateLimitConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    val methodName: String,

    @Enumerated(EnumType.STRING)
    val subscriptionTier: SubscriptionTier,

    @Enumerated(EnumType.STRING)
    val scope: RateLimitScope = RateLimitScope.INDIVIDUAL,

    val capacity: Int,

    val timeValue: Int,

    @Enumerated(EnumType.STRING)
    val timeUnit: ChronoUnit,

    val active: Boolean = true,

) : AuditableEntity()
