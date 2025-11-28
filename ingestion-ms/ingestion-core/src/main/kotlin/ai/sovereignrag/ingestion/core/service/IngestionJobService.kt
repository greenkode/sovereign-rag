package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadRequest
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class IngestionJobService(
    private val jobRepository: IngestionJobRepository,
    private val fileUploadGateway: FileUploadGateway,
    private val jobQueue: JobQueue,
    private val tenantQuotaService: TenantQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val messageSource: MessageSource
) {

    fun createPresignedUpload(tenantId: UUID, request: PresignedUploadRequest): PresignedUploadResponse {
        validateMimeType(request.contentType)

        val validationResult = tenantQuotaService.validateUploadRequest(tenantId, request.fileSize)

        when (validationResult) {
            is QuotaValidationResult.FileSizeExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.file.size.exceeded", validationResult.maxSize / (1024 * 1024))
            )
            is QuotaValidationResult.StorageQuotaExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.storage.quota.exceeded")
            )
            is QuotaValidationResult.MonthlyLimitExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.monthly.limit.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.ConcurrentJobsExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.concurrent.jobs.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.Valid -> {}
        }

        val priority = (validationResult as QuotaValidationResult.Valid).priority

        val job = IngestionJob(
            tenantId = tenantId,
            jobType = JobType.FILE_UPLOAD,
            knowledgeBaseId = request.knowledgeBaseId,
            priority = priority
        ).apply {
            status = JobStatus.UPLOADING
            sourceType = SourceType.PRESIGNED_UPLOAD
            fileName = request.fileName
            fileSize = request.fileSize
            mimeType = request.contentType
        }

        val savedJob = jobRepository.save(job)

        val presignedResult = fileUploadGateway.generatePresignedUploadUrl(
            fileName = request.fileName,
            contentType = request.contentType,
            category = ingestionProperties.storage.uploadsPrefix.trimEnd('/'),
            tenantId = tenantId,
            expirationMinutes = ingestionProperties.limits.presignedUrlExpiryMinutes
        )

        savedJob.sourceReference = presignedResult.key
        jobRepository.save(savedJob)

        log.info { "Created presigned upload for tenant $tenantId, job ${savedJob.id}, priority $priority" }

        return PresignedUploadResponse(
            jobId = savedJob.id!!,
            uploadUrl = presignedResult.uploadUrl,
            key = presignedResult.key,
            expiresIn = presignedResult.expiresIn
        )
    }

    fun confirmUpload(tenantId: UUID, jobId: UUID) {
        val job = getJobForTenant(tenantId, jobId)

        require(job.status == JobStatus.UPLOADING) {
            getMessage("ingestion.error.job.not.uploading")
        }

        jobQueue.enqueue(job)

        log.info { "Confirmed upload and queued job ${job.id} for processing" }
    }

    fun getJob(tenantId: UUID, jobId: UUID): IngestionJobResponse {
        val job = getJobForTenant(tenantId, jobId)
        return mapToResponse(job)
    }

    fun listJobs(
        tenantId: UUID,
        status: JobStatus? = null,
        knowledgeBaseId: UUID? = null,
        page: Int = 0,
        size: Int = 20
    ): Page<IngestionJobResponse> {
        val pageable = PageRequest.of(page, size)

        val jobs = when {
            status != null -> jobRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable)
            knowledgeBaseId != null -> jobRepository.findByTenantIdAndKnowledgeBaseIdOrderByCreatedAtDesc(tenantId, knowledgeBaseId, pageable)
            else -> jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
        }

        return jobs.map { mapToResponse(it) }
    }

    fun cancelJob(tenantId: UUID, jobId: UUID) {
        val job = getJobForTenant(tenantId, jobId)

        require(job.status in listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED)) {
            getMessage("ingestion.error.cannot.cancel", job.status.name)
        }

        job.markCancelled()
        jobRepository.save(job)

        log.info { "Cancelled job $jobId for tenant $tenantId" }
    }

    fun retryJob(tenantId: UUID, jobId: UUID) {
        val job = getJobForTenant(tenantId, jobId)

        require(job.canRetry()) {
            getMessage("ingestion.error.cannot.retry")
        }

        jobQueue.retry(jobId)

        log.info { "Retrying job $jobId for tenant $tenantId, attempt ${job.retryCount + 1}" }
    }

    fun getActiveJobsCount(tenantId: UUID): Long {
        return jobRepository.countActiveJobsForTenant(
            tenantId,
            listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED, JobStatus.PROCESSING)
        )
    }

    private fun getJobForTenant(tenantId: UUID, jobId: UUID): IngestionJob {
        val job = jobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(job.tenantId == tenantId) {
            getMessage("ingestion.error.job.not.owned")
        }

        return job
    }

    private fun validateMimeType(mimeType: String) {
        require(mimeType in ingestionProperties.processing.supportedMimeTypes) {
            getMessage("ingestion.error.unsupported.type", mimeType)
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
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

class IngestionQuotaException(message: String) : RuntimeException(message)
