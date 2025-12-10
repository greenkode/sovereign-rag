package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.audit.IngestionAuditEventPublisher
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ConfirmUploadCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val messageSource: MessageSource,
    private val auditEventPublisher: IngestionAuditEventPublisher
) : Command.Handler<ConfirmUploadCommand, IngestionJobResponse> {

    override fun handle(command: ConfirmUploadCommand): IngestionJobResponse {
        log.info { "Processing ConfirmUploadCommand for job ${command.jobId}" }

        val job = jobRepository.findById(command.jobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(job.organizationId == command.organizationId) {
            getMessage("ingestion.error.job.not.owned")
        }

        require(job.status == JobStatus.UPLOADING) {
            getMessage("ingestion.error.job.not.uploading")
        }

        jobQueue.enqueue(job)

        log.info { "Confirmed upload and queued job ${job.id} for processing" }

        auditEventPublisher.publishJobInitiated(
            eventType = IngestionEventType.FILE_UPLOAD_CONFIRMED,
            organizationId = job.organizationId,
            jobId = job.id!!,
            knowledgeBaseId = job.knowledgeBaseId
        )

        return IngestionJobResponse(
            id = job.id!!,
            organizationId = job.organizationId,
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

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
