package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.scraping.ScrapingStrategy
import ai.sovereignrag.ingestion.core.scraping.ScrapingUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class WebScrapeProcessor(
    private val strategies: List<ScrapingStrategy>,
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val knowledgeSourceGateway: KnowledgeSourceGateway,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.WEB_SCRAPE

    override fun process(job: IngestionJob) {
        log.info { "Processing web scrape job ${job.id}: ${job.sourceReference}" }

        val url = job.sourceReference
            ?: throw IllegalStateException("No URL for web scrape job ${job.id}")

        val metadata = parseMetadata(job)

        updateProgress(job, 10)

        val strategy = findStrategy(metadata.mode)
        val scrapedPages = strategy.scrape(job, url, metadata) { progress ->
            updateProgress(job, progress)
        }

        updateProgress(job, 70)

        job.knowledgeBaseId?.let { kbId ->
            processScrapedPages(job, kbId, scrapedPages)
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        val totalContent = scrapedPages.sumOf { it.content.length }
        job.markCompleted(
            chunksCreated = 0,
            bytesProcessed = totalContent.toLong()
        )
        jobRepository.save(job)

        log.info { "Completed web scrape job ${job.id}: scraped ${scrapedPages.size} pages" }
    }

    private fun findStrategy(mode: ScrapeMode): ScrapingStrategy {
        return strategies.find { it.supports(mode) }
            ?: throw IllegalStateException("No scraping strategy found for mode: $mode")
    }

    private fun parseMetadata(job: IngestionJob): WebScrapeJobMetadata {
        return job.metadata?.let {
            runCatching { objectMapper.readValue<WebScrapeJobMetadata>(it) }
                .getOrElse { parseLegacyMetadata(job.metadata!!) }
        } ?: WebScrapeJobMetadata()
    }

    private fun parseLegacyMetadata(metadata: String): WebScrapeJobMetadata {
        val map = objectMapper.readValue<Map<String, Any>>(metadata)
        val crawl = map["crawl"] as? Boolean ?: false
        val maxDepth = (map["maxDepth"] as? Number)?.toInt() ?: 2
        val maxPages = (map["maxPages"] as? Number)?.toInt() ?: 10

        return WebScrapeJobMetadata(
            mode = crawl.takeIf { it }?.let { ScrapeMode.CRAWL } ?: ScrapeMode.SINGLE,
            maxDepth = maxDepth,
            maxPages = maxPages
        )
    }

    private fun processScrapedPages(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        pages: List<ScrapedPage>
    ) {
        pages.forEach { page ->
            val knowledgeSourceId = createKnowledgeSource(job, knowledgeBaseId, page)
            val chunks = ScrapingUtils.chunkContent(
                page.content,
                ingestionProperties.processing.chunkSize,
                ingestionProperties.processing.chunkOverlap
            )

            createEmbeddingJob(job, chunks, knowledgeSourceId, page)

            log.debug { "Created embedding job for ${page.url} with ${chunks.size} chunks" }
        }

        updateProgress(job, 90)
    }

    private fun createKnowledgeSource(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        page: ScrapedPage
    ): UUID {
        val request = CreateKnowledgeSourceRequest(
            sourceType = SourceType.URL,
            fileName = null,
            sourceUrl = page.url,
            title = page.title.takeIf { it.isNotBlank() } ?: page.url,
            mimeType = "text/html",
            fileSize = page.content.length.toLong(),
            s3Key = null,
            ingestionJobId = job.id,
            metadata = mapOf(
                "depth" to page.depth,
                "parentJobId" to job.id.toString()
            )
        )

        return knowledgeSourceGateway.create(knowledgeBaseId, request).id
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID,
        page: ScrapedPage
    ) {
        val chunkData = ChunkJobData(
            chunks = chunks.mapIndexed { index, content -> ChunkInfo(index, content) },
            sourceType = "URL",
            fileName = null,
            sourceUrl = page.url,
            title = page.title.takeIf { it.isNotBlank() } ?: page.url
        )

        val embeddingJob = IngestionJob(
            organizationId = parentJob.organizationId,
            jobType = JobType.EMBEDDING,
            knowledgeBaseId = parentJob.knowledgeBaseId,
            priority = parentJob.priority
        ).apply {
            parentJobId = parentJob.id
            this.knowledgeSourceId = knowledgeSourceId
            metadata = objectMapper.writeValueAsString(chunkData)
            sourceReference = page.url
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
