package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
import ai.sovereignrag.ingestion.commons.dto.WebScrapeRequest
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class WebScrapeService(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val tenantQuotaService: TenantQuotaService,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource
) {

    fun submitScrapeJob(tenantId: UUID, request: WebScrapeRequest): IngestionJobResponse {
        validateUrl(request.url)

        val validationResult = tenantQuotaService.validateUploadRequest(tenantId, 0)

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
            "crawl" to request.crawl,
            "maxDepth" to request.maxDepth,
            "maxPages" to request.maxPages
        )

        val job = IngestionJob(
            tenantId = tenantId,
            jobType = JobType.WEB_SCRAPE,
            knowledgeBaseId = request.knowledgeBaseId,
            priority = priority
        ).apply {
            sourceType = SourceType.URL
            sourceReference = request.url
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)
        jobQueue.enqueue(savedJob)

        log.info { "Submitted web scrape job ${savedJob.id} for URL ${request.url}, priority $priority" }

        return mapToResponse(savedJob)
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

    private fun mapToResponse(job: IngestionJob): IngestionJobResponse {
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
}
