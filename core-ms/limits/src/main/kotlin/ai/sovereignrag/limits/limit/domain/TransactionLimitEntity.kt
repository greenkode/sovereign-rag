package ai.sovereignrag.limits.limit.domain

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "subscription_limit")
class SubscriptionLimitEntity(

    val organizationId: UUID,

    @Enumerated(EnumType.STRING)
    val subscriptionTier: SubscriptionTier,

    val dailyTokenLimit: Long,

    val monthlyTokenLimit: Long,

    val start: Instant,

    val expiry: Instant?,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
) : AuditableEntity() {

    fun toDomain() = SubscriptionLimit(
        id!!,
        organizationId,
        subscriptionTier,
        dailyTokenLimit,
        monthlyTokenLimit,
        start,
        expiry
    )
}