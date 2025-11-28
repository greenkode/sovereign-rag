package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetJobQueryHandler(
    private val jobRepository: IngestionJobRepository,
    private val messageSource: MessageSource
) : Command.Handler<GetJobQuery, IngestionJobResponse> {

    override fun handle(command: GetJobQuery): IngestionJobResponse {
        log.info { "Processing GetJobQuery for job ${command.jobId}" }

        val job = jobRepository.findById(command.jobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(job.tenantId == command.tenantId) {
            getMessage("ingestion.error.job.not.owned")
        }

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

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
