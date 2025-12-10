package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.audit.IngestionAuditPayloadKey
import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.audit.IngestionAuditEventPublisher
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
class CreatePresignedUploadCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val fileUploadGateway: FileUploadGateway,
    private val organizationQuotaService: OrganizationQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val messageSource: MessageSource,
    private val auditEventPublisher: IngestionAuditEventPublisher
) : Command.Handler<CreatePresignedUploadCommand, PresignedUploadResponse> {

    override fun handle(command: CreatePresignedUploadCommand): PresignedUploadResponse {
        log.info { "Processing CreatePresignedUploadCommand for organization ${command.organizationId}" }

        validateMimeType(command.contentType)

        val validationResult = organizationQuotaService.validateUploadRequest(command.organizationId, command.fileSize)

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
            organizationId = command.organizationId,
            jobType = JobType.FILE_UPLOAD,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            status = JobStatus.UPLOADING
            sourceType = SourceType.PRESIGNED_UPLOAD
            fileName = command.fileName
            fileSize = command.fileSize
            mimeType = command.contentType
        }

        val savedJob = jobRepository.save(job)

        val presignedResult = fileUploadGateway.generatePresignedUploadUrl(
            fileName = command.fileName,
            contentType = command.contentType,
            category = ingestionProperties.storage.uploadsPrefix.trimEnd('/'),
            ownerId = command.organizationId,
            expirationMinutes = ingestionProperties.limits.presignedUrlExpiryMinutes
        )

        savedJob.sourceReference = presignedResult.key
        jobRepository.save(savedJob)

        log.info { "Created presigned upload for organization ${command.organizationId}, job ${savedJob.id}, priority $priority" }

        auditEventPublisher.publishJobInitiated(
            eventType = IngestionEventType.FILE_UPLOAD_INITIATED,
            organizationId = command.organizationId,
            jobId = savedJob.id!!,
            knowledgeBaseId = command.knowledgeBaseId,
            additionalPayload = mapOf(
                IngestionAuditPayloadKey.FILE_NAME.value to command.fileName,
                IngestionAuditPayloadKey.FILE_SIZE.value to command.fileSize.toString(),
                IngestionAuditPayloadKey.MIME_TYPE.value to command.contentType
            )
        )

        return PresignedUploadResponse(
            jobId = savedJob.id!!,
            uploadUrl = presignedResult.uploadUrl,
            key = presignedResult.key,
            expiresIn = presignedResult.expiresIn
        )
    }

    private fun validateMimeType(mimeType: String) {
        require(mimeType in ingestionProperties.processing.supportedMimeTypes) {
            getMessage("ingestion.error.unsupported.type", mimeType)
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}

class IngestionQuotaException(message: String) : RuntimeException(message)
