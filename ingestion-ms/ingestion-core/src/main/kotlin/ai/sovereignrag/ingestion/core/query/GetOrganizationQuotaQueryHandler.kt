package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.OrganizationQuotaResponse
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetOrganizationQuotaQueryHandler(
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties
) : Command.Handler<GetOrganizationQuotaQuery, OrganizationQuotaResponse> {

    override fun handle(command: GetOrganizationQuotaQuery): OrganizationQuotaResponse {
        log.info { "Processing GetOrganizationQuotaQuery for organization ${command.organizationId}" }

        val activeJobsCount = jobRepository.countActiveJobsForOrganization(
            command.organizationId,
            listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED, JobStatus.PROCESSING)
        )

        val totalBytesProcessed = jobRepository.getTotalBytesProcessedForOrganization(command.organizationId) ?: 0L

        return OrganizationQuotaResponse(
            organizationId = command.organizationId,
            maxFileSize = ingestionProperties.limits.maxFileSize,
            maxConcurrentJobs = ingestionProperties.limits.maxConcurrentJobsPerOrganization,
            activeJobsCount = activeJobsCount,
            totalBytesProcessed = totalBytesProcessed,
            storageQuota = null,
            storageUsed = null
        )
    }
}
