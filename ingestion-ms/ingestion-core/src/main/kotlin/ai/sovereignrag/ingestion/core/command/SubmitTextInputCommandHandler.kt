package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.audit.IngestionAuditEventPublisher
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
class SubmitTextInputCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val organizationQuotaService: OrganizationQuotaService,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource,
    private val auditEventPublisher: IngestionAuditEventPublisher
) : Command.Handler<SubmitTextInputCommand, IngestionJobResponse> {

    override fun handle(command: SubmitTextInputCommand): IngestionJobResponse {
        log.info { "Processing SubmitTextInputCommand for organization ${command.organizationId}" }

        validateContent(command.content)

        val contentSize = command.content.length.toLong()
        val validationResult = organizationQuotaService.validateUploadRequest(command.organizationId, contentSize)

        when (validationResult) {
            is QuotaValidationResult.MonthlyLimitExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.monthly.limit.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.ConcurrentJobsExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.concurrent.jobs.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.FileSizeExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.content.too.large", validationResult.maxSize)
            )
            is QuotaValidationResult.Valid -> {}
            else -> {}
        }

        val priority = (validationResult as? QuotaValidationResult.Valid)?.priority ?: 0

        val metadata = buildMap {
            put("title", command.title ?: "Text Input")
            put("contentLength", command.content.length)
            command.metadata?.let { putAll(it) }
        }

        val job = IngestionJob(
            organizationId = command.organizationId,
            jobType = JobType.TEXT_INPUT,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            sourceType = SourceType.TEXT
            sourceReference = command.content
            fileName = command.title ?: "text-input"
            fileSize = contentSize
            mimeType = "text/plain"
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)
        jobQueue.enqueue(savedJob)

        log.info { "Submitted text input job ${savedJob.id}, content size: $contentSize bytes, priority: $priority" }

        auditEventPublisher.publishJobInitiated(
            eventType = IngestionEventType.TEXT_INPUT_SUBMITTED,
            organizationId = command.organizationId,
            jobId = savedJob.id!!,
            knowledgeBaseId = command.knowledgeBaseId
        )

        return IngestionJobResponse(
            id = savedJob.id!!,
            organizationId = savedJob.organizationId,
            knowledgeBaseId = savedJob.knowledgeBaseId,
            jobType = savedJob.jobType,
            status = savedJob.status,
            sourceType = savedJob.sourceType,
            fileName = savedJob.fileName,
            fileSize = savedJob.fileSize,
            mimeType = savedJob.mimeType,
            progress = savedJob.progress,
            errorMessage = savedJob.errorMessage,
            retryCount = savedJob.retryCount,
            chunksCreated = savedJob.chunksCreated,
            bytesProcessed = savedJob.bytesProcessed,
            createdAt = savedJob.createdAt(),
            startedAt = savedJob.startedAt,
            completedAt = savedJob.completedAt,
            processingDurationMs = savedJob.processingDurationMs
        )
    }

    private fun validateContent(content: String) {
        require(content.isNotBlank()) {
            getMessage("ingestion.error.content.empty")
        }
        require(content.length >= 10) {
            getMessage("ingestion.error.content.too.short")
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
