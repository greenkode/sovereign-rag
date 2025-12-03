package ai.sovereignrag.ingestion.commons.entity

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.subscription.SubscriptionTier
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

@Entity
@Table(name = "organization_quotas", schema = "ingestion")
class OrganizationQuota() : AuditableEntity() {
    @Id
    var organizationId: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    var tier: SubscriptionTier = SubscriptionTier.TRIAL

    var storageUsedBytes: Long = 0

    var storageQuotaBytes: Long = 100 * 1024 * 1024

    var monthlyJobsUsed: Int = 0

    var monthlyJobLimit: Int = 50

    var currentPeriodStart: Instant = Instant.now()

    var lastJobCompletedAt: Instant? = null

    constructor(organizationId: UUID, tier: SubscriptionTier = SubscriptionTier.TRIAL) : this() {
        this.organizationId = organizationId
        this.tier = tier
    }

    fun incrementStorageUsed(bytes: Long) {
        storageUsedBytes += bytes
    }

    fun decrementStorageUsed(bytes: Long) {
        storageUsedBytes = (storageUsedBytes - bytes).coerceAtLeast(0)
    }

    fun incrementMonthlyJobs() {
        monthlyJobsUsed++
        lastJobCompletedAt = Instant.now()
    }

    fun hasStorageCapacity(additionalBytes: Long): Boolean {
        return storageUsedBytes + additionalBytes <= storageQuotaBytes
    }

    fun hasJobCapacity(): Boolean {
        return monthlyJobsUsed < monthlyJobLimit
    }

    fun storageRemainingBytes(): Long {
        return (storageQuotaBytes - storageUsedBytes).coerceAtLeast(0)
    }

    fun resetMonthlyUsage() {
        monthlyJobsUsed = 0
        currentPeriodStart = Instant.now()
    }

    fun shouldResetMonthly(): Boolean {
        val startMonth = YearMonth.from(currentPeriodStart.atZone(java.time.ZoneOffset.UTC))
        val currentMonth = YearMonth.now(java.time.ZoneOffset.UTC)
        return currentMonth.isAfter(startMonth)
    }
}
