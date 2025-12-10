package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.QAPair
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
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
class SubmitQAPairsCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val organizationQuotaService: OrganizationQuotaService,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource
) : Command.Handler<SubmitQAPairsCommand, IngestionJobResponse> {

    override fun handle(command: SubmitQAPairsCommand): IngestionJobResponse {
        log.info { "Processing SubmitQAPairsCommand for organization ${command.organizationId}, ${command.pairs.size} pairs" }

        validatePairs(command.pairs)

        val totalContentSize = command.pairs.sumOf { it.question.length + it.answer.length }.toLong()
        val validationResult = organizationQuotaService.validateUploadRequest(command.organizationId, totalContentSize)

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

        val metadata = QAPairsJobMetadata(
            pairs = command.pairs,
            sourceName = command.sourceName ?: "Q&A Import",
            pairCount = command.pairs.size
        )

        val job = IngestionJob(
            organizationId = command.organizationId,
            jobType = JobType.QA_IMPORT,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            sourceType = SourceType.QA_PAIR
            fileName = command.sourceName ?: "qa-pairs"
            fileSize = totalContentSize
            mimeType = "application/json"
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)
        jobQueue.enqueue(savedJob)

        log.info { "Submitted Q&A pairs job ${savedJob.id}, ${command.pairs.size} pairs, priority: $priority" }

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

    private fun validatePairs(pairs: List<QAPair>) {
        require(pairs.isNotEmpty()) {
            getMessage("ingestion.error.qa.pairs.empty")
        }
        require(pairs.size <= 1000) {
            getMessage("ingestion.error.qa.pairs.too.many", 1000)
        }
        pairs.forEachIndexed { index, pair ->
            require(pair.question.isNotBlank()) {
                getMessage("ingestion.error.qa.question.empty", index)
            }
            require(pair.answer.isNotBlank()) {
                getMessage("ingestion.error.qa.answer.empty", index)
            }
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}

data class QAPairsJobMetadata(
    val pairs: List<QAPair>,
    val sourceName: String,
    val pairCount: Int
)
