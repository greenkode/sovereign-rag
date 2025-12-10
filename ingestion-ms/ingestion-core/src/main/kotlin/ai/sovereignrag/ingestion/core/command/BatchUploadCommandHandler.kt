package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.BatchFileUploadInfo
import ai.sovereignrag.ingestion.commons.dto.BatchUploadResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.service.OrganizationQuotaService
import ai.sovereignrag.ingestion.core.service.QuotaValidationResult
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class BatchUploadCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val fileUploadGateway: FileUploadGateway,
    private val organizationQuotaService: OrganizationQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val messageSource: MessageSource
) : Command.Handler<BatchUploadCommand, BatchUploadResponse> {

    override fun handle(command: BatchUploadCommand): BatchUploadResponse {
        log.info { "Processing BatchUploadCommand for organization ${command.organizationId}, ${command.files.size} files" }

        validateBatchSize(command.files.size)
        command.files.forEach { validateMimeType(it.contentType) }

        val totalSize = command.files.sumOf { it.fileSize }
        val validationResult = validateQuotaForBatch(command.organizationId, totalSize, command.files.size)

        val priority = (validationResult as QuotaValidationResult.Valid).priority

        val batchJob = IngestionJob(
            organizationId = command.organizationId,
            jobType = JobType.BATCH_IMPORT,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            status = JobStatus.UPLOADING
            sourceType = SourceType.PRESIGNED_UPLOAD
            fileName = "Batch upload (${command.files.size} files)"
            fileSize = totalSize
        }

        val savedBatchJob = jobRepository.save(batchJob)
        log.info { "Created batch job ${savedBatchJob.id} for ${command.files.size} files" }

        val fileUploadInfos = command.files.map { fileInfo ->
            val childJob = IngestionJob(
                organizationId = command.organizationId,
                jobType = JobType.FILE_UPLOAD,
                knowledgeBaseId = command.knowledgeBaseId,
                priority = priority
            ).apply {
                parentJobId = savedBatchJob.id
                status = JobStatus.UPLOADING
                sourceType = SourceType.PRESIGNED_UPLOAD
                fileName = fileInfo.fileName
                fileSize = fileInfo.fileSize
                mimeType = fileInfo.contentType
            }

            val savedChildJob = jobRepository.save(childJob)

            val presignedResult = fileUploadGateway.generatePresignedUploadUrl(
                fileName = fileInfo.fileName,
                contentType = fileInfo.contentType,
                category = ingestionProperties.storage.uploadsPrefix.trimEnd('/'),
                ownerId = command.organizationId,
                expirationMinutes = ingestionProperties.limits.presignedUrlExpiryMinutes
            )

            savedChildJob.sourceReference = presignedResult.key
            jobRepository.save(savedChildJob)

            BatchFileUploadInfo(
                jobId = savedChildJob.id!!,
                fileName = fileInfo.fileName,
                uploadUrl = presignedResult.uploadUrl,
                key = presignedResult.key
            )
        }

        log.info { "Created ${fileUploadInfos.size} child jobs for batch ${savedBatchJob.id}" }

        return BatchUploadResponse(
            batchJobId = savedBatchJob.id!!,
            files = fileUploadInfos,
            expiresIn = ingestionProperties.limits.presignedUrlExpiryMinutes * 60,
            totalFiles = command.files.size
        )
    }

    private fun validateBatchSize(size: Int) {
        require(size > 0) {
            getMessage("ingestion.error.batch.empty")
        }
        require(size <= ingestionProperties.limits.maxBatchSize) {
            getMessage("ingestion.error.batch.too.large", ingestionProperties.limits.maxBatchSize)
        }
    }

    private fun validateMimeType(mimeType: String) {
        require(mimeType in ingestionProperties.processing.supportedMimeTypes) {
            getMessage("ingestion.error.unsupported.type", mimeType)
        }
    }

    private fun validateQuotaForBatch(organizationId: java.util.UUID, totalSize: Long, fileCount: Int): QuotaValidationResult {
        val quota = organizationQuotaService.getOrCreateQuota(organizationId)
        val tierLimits = organizationQuotaService.getTierLimits(quota.tier)

        if (!quota.hasStorageCapacity(totalSize)) {
            throw IngestionQuotaException(getMessage("ingestion.error.storage.quota.exceeded"))
        }

        if (quota.monthlyJobsUsed + fileCount > quota.monthlyJobLimit) {
            throw IngestionQuotaException(
                getMessage("ingestion.error.monthly.limit.exceeded", quota.monthlyJobLimit)
            )
        }

        val validationResult = organizationQuotaService.validateUploadRequest(organizationId, 0)

        when (validationResult) {
            is QuotaValidationResult.ConcurrentJobsExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.concurrent.jobs.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.MonthlyLimitExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.monthly.limit.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.StorageQuotaExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.storage.quota.exceeded")
            )
            is QuotaValidationResult.FileSizeExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.file.size.exceeded", validationResult.maxSize / (1024 * 1024))
            )
            is QuotaValidationResult.Valid -> return validationResult
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
