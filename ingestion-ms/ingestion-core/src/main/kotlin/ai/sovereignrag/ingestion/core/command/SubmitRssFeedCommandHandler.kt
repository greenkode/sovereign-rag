package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.dto.IngestionJobResponse
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
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
@Transactional
class SubmitRssFeedCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val organizationQuotaService: OrganizationQuotaService,
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource
) : Command.Handler<SubmitRssFeedCommand, IngestionJobResponse> {

    override fun handle(command: SubmitRssFeedCommand): IngestionJobResponse {
        log.info { "Processing SubmitRssFeedCommand for organization ${command.organizationId}, feed: ${command.feedUrl}" }

        validateFeedUrl(command.feedUrl)

        val validationResult = organizationQuotaService.validateUploadRequest(command.organizationId, 0)

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

        val metadata = RssFeedJobMetadata(
            feedUrl = command.feedUrl,
            sourceName = command.sourceName ?: extractFeedName(command.feedUrl),
            maxItems = command.maxItems,
            includeFullContent = command.includeFullContent
        )

        val job = IngestionJob(
            organizationId = command.organizationId,
            jobType = JobType.RSS_FEED,
            knowledgeBaseId = command.knowledgeBaseId,
            priority = priority
        ).apply {
            sourceType = SourceType.RSS_FEED
            sourceReference = command.feedUrl
            fileName = command.sourceName ?: extractFeedName(command.feedUrl)
            mimeType = "application/rss+xml"
            this.metadata = objectMapper.writeValueAsString(metadata)
        }

        val savedJob = jobRepository.save(job)
        jobQueue.enqueue(savedJob)

        log.info { "Submitted RSS feed job ${savedJob.id} for feed ${command.feedUrl}, priority: $priority" }

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

    private fun validateFeedUrl(feedUrl: String) {
        runCatching {
            val uri = URI.create(feedUrl)
            require(uri.scheme in listOf("http", "https")) {
                getMessage("ingestion.error.invalid.feed.url.scheme")
            }
            require(uri.host != null) {
                getMessage("ingestion.error.invalid.feed.url")
            }
        }.getOrElse {
            throw IllegalArgumentException(getMessage("ingestion.error.invalid.feed.url", feedUrl))
        }
    }

    private fun extractFeedName(feedUrl: String): String {
        return runCatching {
            val uri = URI.create(feedUrl)
            uri.host ?: "RSS Feed"
        }.getOrElse { "RSS Feed" }
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}

data class RssFeedJobMetadata(
    val feedUrl: String,
    val sourceName: String,
    val maxItems: Int,
    val includeFullContent: Boolean
)
