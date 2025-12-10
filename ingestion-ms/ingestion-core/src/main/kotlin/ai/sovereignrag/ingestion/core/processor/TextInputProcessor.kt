package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class TextInputProcessor(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val knowledgeSourceGateway: KnowledgeSourceGateway,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.TEXT_INPUT

    override fun process(job: IngestionJob) {
        log.info { "Processing text input job ${job.id}" }

        val content = job.sourceReference
            ?: throw IllegalStateException("No content for text input job ${job.id}")

        updateProgress(job, 10)

        val metadata = parseMetadata(job)
        val title = metadata["title"] as? String ?: "Text Input"

        updateProgress(job, 30)

        val chunks = chunkContent(content)

        updateProgress(job, 50)

        job.knowledgeBaseId?.let { kbId ->
            val knowledgeSourceId = createKnowledgeSource(job, kbId, title, content.length.toLong())
            createEmbeddingJob(job, chunks, knowledgeSourceId, title)
            log.info { "Created embedding job for text input with ${chunks.size} chunks" }
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        updateProgress(job, 90)

        job.markCompleted(
            chunksCreated = chunks.size,
            bytesProcessed = content.length.toLong()
        )
        jobRepository.save(job)

        log.info { "Completed text input job ${job.id}: created ${chunks.size} chunks" }
    }

    private fun parseMetadata(job: IngestionJob): Map<String, Any> {
        return job.metadata?.let {
            runCatching { objectMapper.readValue<Map<String, Any>>(it) }.getOrElse { emptyMap() }
        } ?: emptyMap()
    }

    private fun chunkContent(content: String): List<String> {
        val chunkSize = ingestionProperties.processing.chunkSize
        val overlap = ingestionProperties.processing.chunkOverlap

        if (content.length <= chunkSize) {
            return listOf(content)
        }

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < content.length) {
            val end = (start + chunkSize).coerceAtMost(content.length)
            val chunk = content.substring(start, end)
            chunks.add(chunk)

            start = end - overlap
            if (start >= content.length - overlap) break
        }

        return chunks
    }

    private fun createKnowledgeSource(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        title: String,
        contentSize: Long
    ): UUID {
        val request = CreateKnowledgeSourceRequest(
            sourceType = SourceType.TEXT,
            fileName = null,
            sourceUrl = null,
            title = title,
            mimeType = "text/plain",
            fileSize = contentSize,
            s3Key = null,
            ingestionJobId = job.id,
            metadata = mapOf("type" to "text_input")
        )

        return knowledgeSourceGateway.create(knowledgeBaseId, request).id
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID,
        title: String
    ) {
        val chunkData = ChunkJobData(
            chunks = chunks.mapIndexed { index, content -> ChunkInfo(index, content) },
            sourceType = "TEXT",
            fileName = null,
            sourceUrl = null,
            title = title
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
            fileName = title
            mimeType = "text/plain"
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
