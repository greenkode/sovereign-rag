package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.config.TierLimits
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.TenantQuota
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.commons.repository.TenantQuotaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class TenantQuotaService(
    private val quotaRepository: TenantQuotaRepository,
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties
) {

    fun getOrCreateQuota(tenantId: UUID): TenantQuota {
        return quotaRepository.findByTenantId(tenantId)
            ?.also { quota ->
                if (quota.shouldResetMonthly()) {
                    quota.resetMonthlyUsage()
                    quotaRepository.save(quota)
                    log.info { "Reset monthly quota for tenant $tenantId" }
                }
            }
            ?: createDefaultQuota(tenantId)
    }

    fun getTierLimits(tier: SubscriptionTier): TierLimits {
        return when (tier) {
            SubscriptionTier.TRIAL -> ingestionProperties.tiers.trial
            SubscriptionTier.STARTER -> ingestionProperties.tiers.starter
            SubscriptionTier.PROFESSIONAL -> ingestionProperties.tiers.professional
            SubscriptionTier.ENTERPRISE -> ingestionProperties.tiers.enterprise
        }
    }

    fun validateUploadRequest(tenantId: UUID, fileSize: Long): QuotaValidationResult {
        val quota = getOrCreateQuota(tenantId)
        val tierLimits = getTierLimits(quota.tier)

        if (fileSize > tierLimits.maxFileSize) {
            return QuotaValidationResult.FileSizeExceeded(
                maxSize = tierLimits.maxFileSize,
                requestedSize = fileSize
            )
        }

        if (!quota.hasStorageCapacity(fileSize)) {
            return QuotaValidationResult.StorageQuotaExceeded(
                quotaBytes = quota.storageQuotaBytes,
                usedBytes = quota.storageUsedBytes,
                requestedBytes = fileSize
            )
        }

        if (!quota.hasJobCapacity()) {
            return QuotaValidationResult.MonthlyLimitExceeded(
                limit = quota.monthlyJobLimit,
                used = quota.monthlyJobsUsed
            )
        }

        val activeJobs = jobRepository.countActiveJobsForTenant(
            tenantId,
            listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED, JobStatus.PROCESSING)
        )

        if (activeJobs >= tierLimits.maxConcurrentJobs) {
            return QuotaValidationResult.ConcurrentJobsExceeded(
                limit = tierLimits.maxConcurrentJobs,
                active = activeJobs.toInt()
            )
        }

        return QuotaValidationResult.Valid(
            priority = tierLimits.priority,
            tier = quota.tier
        )
    }

    fun getPriorityForTenant(tenantId: UUID): Int {
        val quota = getOrCreateQuota(tenantId)
        return getTierLimits(quota.tier).priority
    }

    fun recordJobCompletion(tenantId: UUID, bytesProcessed: Long) {
        val quota = getOrCreateQuota(tenantId)
        quota.incrementStorageUsed(bytesProcessed)
        quota.incrementMonthlyJobs()
        quotaRepository.save(quota)
        log.debug { "Recorded job completion for tenant $tenantId: $bytesProcessed bytes" }
    }

    fun recordStorageDecrease(tenantId: UUID, bytesDeleted: Long) {
        val quota = getOrCreateQuota(tenantId)
        quota.decrementStorageUsed(bytesDeleted)
        quotaRepository.save(quota)
        log.debug { "Recorded storage decrease for tenant $tenantId: $bytesDeleted bytes" }
    }

    fun updateTenantTier(tenantId: UUID, newTier: SubscriptionTier) {
        val quota = getOrCreateQuota(tenantId)
        val tierLimits = getTierLimits(newTier)

        quota.tier = newTier
        quota.storageQuotaBytes = tierLimits.storageQuotaBytes
        quota.monthlyJobLimit = tierLimits.monthlyJobLimit

        quotaRepository.save(quota)
        log.info { "Updated tenant $tenantId to tier $newTier" }
    }

    private fun createDefaultQuota(tenantId: UUID): TenantQuota {
        val tierLimits = getTierLimits(SubscriptionTier.TRIAL)
        val quota = TenantQuota(tenantId, SubscriptionTier.TRIAL).apply {
            storageQuotaBytes = tierLimits.storageQuotaBytes
            monthlyJobLimit = tierLimits.monthlyJobLimit
            currentPeriodStart = Instant.now()
        }
        return quotaRepository.save(quota)
    }
}

sealed class QuotaValidationResult {
    data class Valid(val priority: Int, val tier: SubscriptionTier) : QuotaValidationResult()
    data class FileSizeExceeded(val maxSize: Long, val requestedSize: Long) : QuotaValidationResult()
    data class StorageQuotaExceeded(val quotaBytes: Long, val usedBytes: Long, val requestedBytes: Long) : QuotaValidationResult()
    data class MonthlyLimitExceeded(val limit: Int, val used: Int) : QuotaValidationResult()
    data class ConcurrentJobsExceeded(val limit: Int, val active: Int) : QuotaValidationResult()
}
