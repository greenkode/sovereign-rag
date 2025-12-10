package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.dto.QAPair
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.command.QAPairsJobMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class QAPairsProcessor(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val knowledgeSourceGateway: KnowledgeSourceGateway,
    private val objectMapper: ObjectMapper
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.QA_IMPORT

    override fun process(job: IngestionJob) {
        log.info { "Processing Q&A pairs job ${job.id}" }

        val metadata = parseMetadata(job)
        val pairs = metadata.pairs
        val sourceName = metadata.sourceName

        updateProgress(job, 10)

        log.info { "Processing ${pairs.size} Q&A pairs for source: $sourceName" }

        job.knowledgeBaseId?.let { kbId ->
            val chunks = pairs.map { pair -> formatQAPairAsChunk(pair) }
            val totalSize = chunks.sumOf { it.length }.toLong()

            updateProgress(job, 30)

            val knowledgeSourceId = createKnowledgeSource(job, kbId, sourceName, totalSize, pairs.size)

            updateProgress(job, 50)

            createEmbeddingJob(job, chunks, knowledgeSourceId, sourceName, pairs)

            updateProgress(job, 80)

            log.info { "Created embedding job for ${pairs.size} Q&A pairs" }
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        val totalBytes = pairs.sumOf { it.question.length + it.answer.length }.toLong()
        job.markCompleted(
            chunksCreated = pairs.size,
            bytesProcessed = totalBytes
        )
        jobRepository.save(job)

        log.info { "Completed Q&A pairs job ${job.id}: processed ${pairs.size} pairs" }
    }

    private fun parseMetadata(job: IngestionJob): QAPairsJobMetadata {
        val metadata = job.metadata
            ?: throw IllegalStateException("Metadata is required for Q&A pairs job ${job.id}")

        return objectMapper.readValue<QAPairsJobMetadata>(metadata)
    }

    private fun formatQAPairAsChunk(pair: QAPair): String {
        val categoryPart = pair.category?.let { "Category: $it\n" } ?: ""
        val tagsPart = pair.tags?.takeIf { it.isNotEmpty() }?.let { "Tags: ${it.joinToString(", ")}\n" } ?: ""

        return buildString {
            append(categoryPart)
            append(tagsPart)
            append("Question: ${pair.question}\n\n")
            append("Answer: ${pair.answer}")
        }
    }

    private fun createKnowledgeSource(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        sourceName: String,
        totalSize: Long,
        pairCount: Int
    ): UUID {
        val request = CreateKnowledgeSourceRequest(
            sourceType = SourceType.QA_PAIR,
            fileName = null,
            sourceUrl = null,
            title = sourceName,
            mimeType = "application/json",
            fileSize = totalSize,
            s3Key = null,
            ingestionJobId = job.id,
            metadata = mapOf(
                "type" to "qa_pairs",
                "pairCount" to pairCount
            )
        )

        return knowledgeSourceGateway.create(knowledgeBaseId, request).id
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID,
        sourceName: String,
        pairs: List<QAPair>
    ) {
        val chunkInfos = chunks.mapIndexed { index, content ->
            ChunkInfo(
                index = index,
                content = content
            )
        }

        val chunkData = ChunkJobData(
            chunks = chunkInfos,
            sourceType = "QA_PAIR",
            fileName = null,
            sourceUrl = null,
            title = sourceName
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
            fileName = sourceName
            mimeType = "application/json"
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
