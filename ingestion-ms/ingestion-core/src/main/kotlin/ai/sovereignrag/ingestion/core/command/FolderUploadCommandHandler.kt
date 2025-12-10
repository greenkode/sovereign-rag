package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.PresignedUploadResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.service.OrganizationQuotaService
import ai.sovereignrag.ingestion.core.service.QuotaValidationResult
import an.awesome.pipelinr.Command
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class FolderUploadCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val fileUploadGateway: FileUploadGateway,
    private val organizationQuotaService: OrganizationQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource
) : Command.Handler<FolderUploadCommand, PresignedUploadResponse> {

    override fun handle(command: FolderUploadCommand): PresignedUploadResponse {
        log.info { "Processing FolderUploadCommand for organization ${command.organizationId}, file: ${command.fileName}" }

        validateZipFile(command.fileName)

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

        val metadata = FolderImportJobMetadata(
            preserveStructure = command.preserveStructure
        )

        val job = IngestionJob(
            organizationId = command.organizationId,
            jobType = JobType.FOLDER_IMPORT,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            status = JobStatus.UPLOADING
            sourceType = SourceType.ZIP_ARCHIVE
            fileName = command.fileName
            fileSize = command.fileSize
            mimeType = "application/zip"
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)

        val presignedResult = fileUploadGateway.generatePresignedUploadUrl(
            fileName = command.fileName,
            contentType = "application/zip",
            category = ingestionProperties.storage.uploadsPrefix.trimEnd('/'),
            ownerId = command.organizationId,
            expirationMinutes = ingestionProperties.limits.presignedUrlExpiryMinutes
        )

        savedJob.sourceReference = presignedResult.key
        jobRepository.save(savedJob)

        log.info { "Created folder upload job ${savedJob.id} for organization ${command.organizationId}" }

        return PresignedUploadResponse(
            jobId = savedJob.id!!,
            uploadUrl = presignedResult.uploadUrl,
            key = presignedResult.key,
            expiresIn = presignedResult.expiresIn
        )
    }

    private fun validateZipFile(fileName: String) {
        require(fileName.lowercase().endsWith(".zip")) {
            getMessage("ingestion.error.folder.not.zip")
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}

data class FolderImportJobMetadata(
    val preserveStructure: Boolean
)
