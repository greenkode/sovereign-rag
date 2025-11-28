package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.service.QuotaValidationResult
import ai.sovereignrag.ingestion.core.service.TenantQuotaService
import an.awesome.pipelinr.Command
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
@Transactional
class SubmitScrapeJobCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val tenantQuotaService: TenantQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource
) : Command.Handler<SubmitScrapeJobCommand, IngestionJobResponse> {

    override fun handle(command: SubmitScrapeJobCommand): IngestionJobResponse {
        log.info { "Processing SubmitScrapeJobCommand for tenant ${command.tenantId}" }

        validateUrl(command.url)

        val validationResult = tenantQuotaService.validateUploadRequest(command.tenantId, 0)

        when (validationResult) {
            is QuotaValidationResult.MonthlyLimitExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.monthly.limit.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.ConcurrentJobsExceeded -> throw IngestionQuotaException(
                getMessage("ingestion.error.concurrent.jobs.exceeded", validationResult.limit)
            )
            is QuotaValidationResult.Valid -> {}
            else -> {}
        }

        val priority = (validationResult as? QuotaValidationResult.Valid)?.priority ?: 0

        val metadata = mapOf(
            "crawl" to (command.depth > 1),
            "maxDepth" to command.depth,
            "maxPages" to command.maxPages
        )

        val job = IngestionJob(
            tenantId = command.tenantId,
            jobType = JobType.WEB_SCRAPE,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            sourceType = SourceType.URL
            sourceReference = command.url
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)
        jobQueue.enqueue(savedJob)

        log.info { "Submitted web scrape job ${savedJob.id} for URL ${command.url}, priority $priority" }

        return IngestionJobResponse(
            id = savedJob.id!!,
            tenantId = savedJob.tenantId,
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

    private fun validateUrl(url: String) {
        runCatching {
            val uri = URI.create(url)
            require(uri.scheme in listOf("http", "https")) {
                getMessage("ingestion.error.invalid.url.scheme")
            }
            require(uri.host != null) {
                getMessage("ingestion.error.invalid.url")
            }
        }.getOrElse {
            throw IllegalArgumentException(getMessage("ingestion.error.invalid.url", url))
        }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
