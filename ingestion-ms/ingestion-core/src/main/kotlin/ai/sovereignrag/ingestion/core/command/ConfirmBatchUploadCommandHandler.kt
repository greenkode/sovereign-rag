package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
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
class ConfirmBatchUploadCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val messageSource: MessageSource
) : Command.Handler<ConfirmBatchUploadCommand, IngestionJobResponse> {

    override fun handle(command: ConfirmBatchUploadCommand): IngestionJobResponse {
        log.info { "Processing ConfirmBatchUploadCommand for batch job ${command.batchJobId}" }

        val batchJob = jobRepository.findById(command.batchJobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(batchJob.organizationId == command.organizationId) {
            getMessage("ingestion.error.job.not.owned")
        }

        require(batchJob.jobType == JobType.BATCH_IMPORT) {
            getMessage("ingestion.error.job.not.batch")
        }

        require(batchJob.status == JobStatus.UPLOADING) {
            getMessage("ingestion.error.job.not.uploading")
        }

        val childJobs = jobRepository.findByParentJobId(batchJob.id!!)

        require(childJobs.isNotEmpty()) {
            getMessage("ingestion.error.batch.no.files")
        }

        var enqueuedCount = 0
        childJobs.filter { it.status == JobStatus.UPLOADING }.forEach { childJob ->
            jobQueue.enqueue(childJob)
            enqueuedCount++
        }

        batchJob.markProcessing()
        jobRepository.save(batchJob)

        log.info { "Confirmed batch upload ${batchJob.id}, enqueued $enqueuedCount child jobs for processing" }

        return IngestionJobResponse(
            id = batchJob.id!!,
            organizationId = batchJob.organizationId,
            knowledgeBaseId = batchJob.knowledgeBaseId,
            jobType = batchJob.jobType,
            status = batchJob.status,
            sourceType = batchJob.sourceType,
            fileName = batchJob.fileName,
            fileSize = batchJob.fileSize,
            mimeType = batchJob.mimeType,
            progress = batchJob.progress,
            errorMessage = batchJob.errorMessage,
            retryCount = batchJob.retryCount,
            chunksCreated = batchJob.chunksCreated,
            bytesProcessed = batchJob.bytesProcessed,
            createdAt = batchJob.createdAt(),
            startedAt = batchJob.startedAt,
            completedAt = batchJob.completedAt,
            processingDurationMs = batchJob.processingDurationMs
        )
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
