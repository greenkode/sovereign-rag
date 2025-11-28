package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.TenantQuotaResponse
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetTenantQuotaQueryHandler(
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties
) : Command.Handler<GetTenantQuotaQuery, TenantQuotaResponse> {

    override fun handle(command: GetTenantQuotaQuery): TenantQuotaResponse {
        log.info { "Processing GetTenantQuotaQuery for tenant ${command.tenantId}" }

        val activeJobsCount = jobRepository.countActiveJobsForTenant(
            command.tenantId,
            listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED, JobStatus.PROCESSING)
        )

        val totalBytesProcessed = jobRepository.getTotalBytesProcessedForTenant(command.tenantId) ?: 0L

        return TenantQuotaResponse(
            tenantId = command.tenantId,
            maxFileSize = ingestionProperties.limits.maxFileSize,
            maxConcurrentJobs = ingestionProperties.limits.maxConcurrentJobsPerTenant,
            activeJobsCount = activeJobsCount,
            totalBytesProcessed = totalBytesProcessed,
            storageQuota = null,
            storageUsed = null
        )
    }
}
