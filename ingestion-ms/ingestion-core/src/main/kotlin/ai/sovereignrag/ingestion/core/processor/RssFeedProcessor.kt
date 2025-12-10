package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.command.RssFeedJobMetadata
import ai.sovereignrag.ingestion.core.rss.FeedEntry
import ai.sovereignrag.ingestion.core.rss.RssFeedParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class RssFeedProcessor(
    private val rssFeedParser: RssFeedParser,
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val knowledgeSourceGateway: KnowledgeSourceGateway,
    private val objectMapper: ObjectMapper
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.RSS_FEED

    override fun process(job: IngestionJob) {
        log.info { "Processing RSS feed job ${job.id}" }

        val metadata = parseMetadata(job)

        updateProgress(job, 10)

        log.info { "Fetching RSS feed from: ${metadata.feedUrl}" }
        val parsedFeed = rssFeedParser.parse(metadata.feedUrl, metadata.maxItems)

        updateProgress(job, 30)

        log.info { "Parsed ${parsedFeed.entries.size} entries from feed: ${parsedFeed.title}" }

        job.knowledgeBaseId?.let { kbId ->
            val chunks = parsedFeed.entries.map { entry ->
                formatEntryAsChunk(entry, metadata.includeFullContent)
            }

            val totalSize = chunks.sumOf { it.length }.toLong()

            updateProgress(job, 50)

            val knowledgeSourceId = createKnowledgeSource(
                job = job,
                knowledgeBaseId = kbId,
                feedTitle = parsedFeed.title,
                totalSize = totalSize,
                entryCount = parsedFeed.entries.size
            )

            updateProgress(job, 70)

            createEmbeddingJob(
                parentJob = job,
                chunks = chunks,
                knowledgeSourceId = knowledgeSourceId,
                feedTitle = parsedFeed.title
            )

            updateProgress(job, 90)

            log.info { "Created embedding job for ${parsedFeed.entries.size} feed entries" }
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        val totalBytes = parsedFeed.entries.sumOf { it.content.length + it.title.length }.toLong()
        job.markCompleted(
            chunksCreated = parsedFeed.entries.size,
            bytesProcessed = totalBytes
        )
        jobRepository.save(job)

        log.info { "Completed RSS feed job ${job.id}: processed ${parsedFeed.entries.size} entries" }
    }

    private fun parseMetadata(job: IngestionJob): RssFeedJobMetadata {
        val metadata = job.metadata
            ?: throw IllegalStateException("Metadata is required for RSS feed job ${job.id}")

        return objectMapper.readValue<RssFeedJobMetadata>(metadata)
    }

    private fun formatEntryAsChunk(entry: FeedEntry, includeFullContent: Boolean): String {
        return buildString {
            append("Title: ${entry.title}\n\n")

            entry.author?.let { append("Author: $it\n") }
            entry.publishedDate?.let { append("Published: $it\n") }
            entry.link?.let { append("Link: $it\n") }

            if (entry.categories.isNotEmpty()) {
                append("Categories: ${entry.categories.joinToString(", ")}\n")
            }

            append("\n")

            if (includeFullContent && entry.content.isNotBlank()) {
                append(entry.content)
            } else {
                append(entry.description ?: entry.content)
            }
        }
    }

    private fun createKnowledgeSource(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        feedTitle: String,
        totalSize: Long,
        entryCount: Int
    ): UUID {
        val request = CreateKnowledgeSourceRequest(
            sourceType = SourceType.RSS_FEED,
            fileName = null,
            sourceUrl = job.sourceReference,
            title = feedTitle,
            mimeType = "application/rss+xml",
            fileSize = totalSize,
            s3Key = null,
            ingestionJobId = job.id,
            metadata = mapOf(
                "type" to "rss_feed",
                "entryCount" to entryCount,
                "feedUrl" to (job.sourceReference ?: "")
            )
        )

        return knowledgeSourceGateway.create(knowledgeBaseId, request).id
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID,
        feedTitle: String
    ) {
        val chunkInfos = chunks.mapIndexed { index, content ->
            ChunkInfo(
                index = index,
                content = content
            )
        }

        val chunkData = ChunkJobData(
            chunks = chunkInfos,
            sourceType = "RSS_FEED",
            fileName = null,
            sourceUrl = parentJob.sourceReference,
            title = feedTitle
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
            fileName = feedTitle
            mimeType = "application/rss+xml"
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
