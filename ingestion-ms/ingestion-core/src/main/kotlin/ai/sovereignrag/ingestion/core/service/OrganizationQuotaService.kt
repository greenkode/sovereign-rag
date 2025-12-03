package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.config.TierLimits
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.OrganizationQuota
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.commons.repository.OrganizationQuotaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class OrganizationQuotaService(
    private val quotaRepository: OrganizationQuotaRepository,
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties
) {

    fun getOrCreateQuota(organizationId: UUID): OrganizationQuota {
        return quotaRepository.findByOrganizationId(organizationId)
            ?.also { quota ->
                if (quota.shouldResetMonthly()) {
                    quota.resetMonthlyUsage()
                    quotaRepository.save(quota)
                    log.info { "Reset monthly quota for organization $organizationId" }
                }
            }
            ?: createDefaultQuota(organizationId)
    }

    fun getTierLimits(tier: SubscriptionTier): TierLimits {
        return when (tier) {
            SubscriptionTier.TRIAL -> ingestionProperties.tiers.trial
            SubscriptionTier.STARTER -> ingestionProperties.tiers.starter
            SubscriptionTier.PROFESSIONAL -> ingestionProperties.tiers.professional
            SubscriptionTier.ENTERPRISE -> ingestionProperties.tiers.enterprise
        }
    }

    fun validateUploadRequest(organizationId: UUID, fileSize: Long): QuotaValidationResult {
        val quota = getOrCreateQuota(organizationId)
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

        val activeJobs = jobRepository.countActiveJobsForOrganization(
            organizationId,
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

    fun getPriorityForOrganization(organizationId: UUID): Int {
        val quota = getOrCreateQuota(organizationId)
        return getTierLimits(quota.tier).priority
    }

    fun recordJobCompletion(organizationId: UUID, bytesProcessed: Long) {
        val quota = getOrCreateQuota(organizationId)
        quota.incrementStorageUsed(bytesProcessed)
        quota.incrementMonthlyJobs()
        quotaRepository.save(quota)
        log.debug { "Recorded job completion for organization $organizationId: $bytesProcessed bytes" }
    }

    fun recordStorageDecrease(organizationId: UUID, bytesDeleted: Long) {
        val quota = getOrCreateQuota(organizationId)
        quota.decrementStorageUsed(bytesDeleted)
        quotaRepository.save(quota)
        log.debug { "Recorded storage decrease for organization $organizationId: $bytesDeleted bytes" }
    }

    fun updateOrganizationTier(organizationId: UUID, newTier: SubscriptionTier) {
        val quota = getOrCreateQuota(organizationId)
        val tierLimits = getTierLimits(newTier)

        quota.tier = newTier
        quota.storageQuotaBytes = tierLimits.storageQuotaBytes
        quota.monthlyJobLimit = tierLimits.monthlyJobLimit

        quotaRepository.save(quota)
        log.info { "Updated organization $organizationId to tier $newTier" }
    }

    private fun createDefaultQuota(organizationId: UUID): OrganizationQuota {
        val tierLimits = getTierLimits(SubscriptionTier.TRIAL)
        val quota = OrganizationQuota(organizationId, SubscriptionTier.TRIAL).apply {
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
