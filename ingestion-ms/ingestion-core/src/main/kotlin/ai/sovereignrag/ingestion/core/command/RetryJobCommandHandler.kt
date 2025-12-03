package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RetryJobCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val messageSource: MessageSource
) : Command.Handler<RetryJobCommand, IngestionJobResponse> {

    override fun handle(command: RetryJobCommand): IngestionJobResponse {
        log.info { "Processing RetryJobCommand for job ${command.jobId}" }

        val job = jobRepository.findById(command.jobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(job.organizationId == command.organizationId) {
            getMessage("ingestion.error.job.not.owned")
        }

        require(job.canRetry()) {
            getMessage("ingestion.error.cannot.retry")
        }

        jobQueue.retry(command.jobId)

        val updatedJob = jobRepository.findById(command.jobId).get()

        log.info { "Retrying job ${command.jobId} for organization ${command.organizationId}, attempt ${updatedJob.retryCount}" }

        return IngestionJobResponse(
            id = updatedJob.id!!,
            organizationId = updatedJob.organizationId,
            knowledgeBaseId = updatedJob.knowledgeBaseId,
            jobType = updatedJob.jobType,
            status = updatedJob.status,
            sourceType = updatedJob.sourceType,
            fileName = updatedJob.fileName,
            fileSize = updatedJob.fileSize,
            mimeType = updatedJob.mimeType,
            progress = updatedJob.progress,
            errorMessage = updatedJob.errorMessage,
            retryCount = updatedJob.retryCount,
            chunksCreated = updatedJob.chunksCreated,
            bytesProcessed = updatedJob.bytesProcessed,
            createdAt = updatedJob.createdAt(),
            startedAt = updatedJob.startedAt,
            completedAt = updatedJob.completedAt,
            processingDurationMs = updatedJob.processingDurationMs
        )
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
