package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.chunking.ChunkingResult
import ai.sovereignrag.ingestion.core.chunking.ChunkingService
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
    private val objectMapper: ObjectMapper,
    private val chunkingService: ChunkingService
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

        val chunkingResult = chunkContent(content)

        updateProgress(job, 50)

        log.info {
            "Chunked text input into ${chunkingResult.chunks.size} chunks " +
            "using strategy: ${chunkingResult.strategyUsed} (${chunkingResult.processingTimeMs}ms)"
        }

        chunkingResult.qualityReport?.let { report ->
            log.info { "Chunking quality score: ${report.overallScore} for text input job ${job.id}" }
        }

        job.knowledgeBaseId?.let { kbId ->
            val knowledgeSourceId = createKnowledgeSource(job, kbId, title, content.length.toLong())
            createEmbeddingJob(job, chunkingResult, knowledgeSourceId, title)
            log.info { "Created embedding job for text input with ${chunkingResult.chunks.size} chunks" }
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        updateProgress(job, 90)

        job.markCompleted(
            chunksCreated = chunkingResult.chunks.size,
            bytesProcessed = content.length.toLong()
        )
        jobRepository.save(job)

        log.info { "Completed text input job ${job.id}: created ${chunkingResult.chunks.size} chunks" }
    }

    private fun parseMetadata(job: IngestionJob): Map<String, Any> {
        return job.metadata?.let {
            runCatching { objectMapper.readValue<Map<String, Any>>(it) }.getOrElse { emptyMap() }
        } ?: emptyMap()
    }

    private fun chunkContent(content: String): ChunkingResult {
        val config = ChunkingConfig(
            chunkSize = ingestionProperties.processing.chunkSize,
            chunkOverlap = ingestionProperties.processing.chunkOverlap
        )
        return chunkingService.chunk(content, "text/plain", config)
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
        chunkingResult: ChunkingResult,
        knowledgeSourceId: UUID,
        title: String
    ) {
        val chunkData = ChunkJobData(
            chunks = chunkingResult.chunks.mapIndexed { index, chunk ->
                ChunkInfo(index, chunk.content)
            },
            sourceType = "TEXT",
            fileName = null,
            sourceUrl = null,
            title = title,
            chunkingStrategy = chunkingResult.strategyUsed,
            qualityScore = chunkingResult.qualityReport?.overallScore
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
