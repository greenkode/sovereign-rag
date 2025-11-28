package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.JobListResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class ListJobsQueryHandler(
    private val jobRepository: IngestionJobRepository
) : Command.Handler<ListJobsQuery, JobListResponse> {

    override fun handle(command: ListJobsQuery): JobListResponse {
        log.info { "Processing ListJobsQuery for tenant ${command.tenantId}" }

        val pageable = PageRequest.of(command.page, command.size)

        val jobs = when {
            command.status != null -> jobRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                command.tenantId, command.status, pageable
            )
            command.knowledgeBaseId != null -> jobRepository.findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(
                command.tenantId, command.knowledgeBaseId, pageable
            )
            else -> jobRepository.findByTenantIdOrderByCreatedAtDesc(command.tenantId, pageable)
        }

        return JobListResponse(
            jobs = jobs.content.map { mapToResponse(it) },
            total = jobs.totalElements,
            page = command.page,
            pageSize = command.size,
            totalPages = jobs.totalPages
        )
    }

    private fun mapToResponse(job: IngestionJob): IngestionJobResponse {
        return IngestionJobResponse(
            id = job.id!!,
            tenantId = job.tenantId,
            knowledgeBaseId = job.knowledgeBaseId,
            jobType = job.jobType,
            status = job.status,
            sourceType = job.sourceType,
            fileName = job.fileName,
            fileSize = job.fileSize,
            mimeType = job.mimeType,
            progress = job.progress,
            errorMessage = job.errorMessage,
            retryCount = job.retryCount,
            chunksCreated = job.chunksCreated,
            bytesProcessed = job.bytesProcessed,
            createdAt = job.createdAt(),
            startedAt = job.startedAt,
            completedAt = job.completedAt,
            processingDurationMs = job.processingDurationMs
        )
    }
}
