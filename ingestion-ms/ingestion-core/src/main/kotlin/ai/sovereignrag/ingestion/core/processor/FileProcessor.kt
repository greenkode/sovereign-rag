package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.chunking.ChunkingResult
import ai.sovereignrag.ingestion.core.chunking.ChunkingService
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class FileProcessor(
    private val fileUploadGateway: FileUploadGateway,
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val objectMapper: ObjectMapper,
    private val ingestionProperties: IngestionProperties,
    private val chunkingService: ChunkingService
) : JobProcessor {
    private val tika = Tika()

    override fun supports(jobType: JobType): Boolean = jobType == JobType.FILE_UPLOAD

    override fun process(job: IngestionJob) {
        log.info { "Processing file job ${job.id}: ${job.fileName}" }

        val sourceKey = job.sourceReference
            ?: throw IllegalStateException("No source reference for job ${job.id}")

        updateProgress(job, 10)

        val inputStream = fileUploadGateway.getFileStream(sourceKey)

        updateProgress(job, 30)

        val content = extractContent(inputStream, job.mimeType)
        updateProgress(job, 50)

        val mimeType = job.mimeType ?: "text/plain"
        val chunkingResult = chunkContent(content, mimeType, job.knowledgeSourceId)
        updateProgress(job, 80)

        log.info {
            "Extracted ${chunkingResult.chunks.size} chunks from ${job.fileName} " +
            "using strategy: ${chunkingResult.strategyUsed} (${chunkingResult.processingTimeMs}ms)"
        }

        chunkingResult.qualityReport?.let { report ->
            log.info { "Chunking quality score: ${report.overallScore} for ${job.fileName}" }
        }

        job.knowledgeBaseId?.let {
            val knowledgeSourceId = job.knowledgeSourceId ?: UUID.randomUUID()
            createEmbeddingJob(job, chunkingResult, knowledgeSourceId)
            log.info { "Created embedding job for ${chunkingResult.chunks.size} chunks" }
        }

        job.markCompleted(
            chunksCreated = chunkingResult.chunks.size,
            bytesProcessed = content.length.toLong()
        )
        jobRepository.save(job)
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunkingResult: ChunkingResult,
        knowledgeSourceId: UUID
    ) {
        val chunkData = ChunkJobData(
            chunks = chunkingResult.chunks.mapIndexed { index, chunk ->
                ChunkInfo(index, chunk.content)
            },
            sourceType = "FILE",
            fileName = parentJob.fileName,
            sourceUrl = null,
            title = parentJob.fileName,
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
            fileName = parentJob.fileName
            mimeType = parentJob.mimeType
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun extractContent(inputStream: InputStream, mimeType: String?): String {
        return inputStream.use { stream ->
            val metadata = Metadata()
            mimeType?.let { metadata.set(Metadata.CONTENT_TYPE, it) }
            tika.parseToString(stream, metadata)
        }
    }

    private fun chunkContent(content: String, mimeType: String, sourceId: UUID?): ChunkingResult {
        val config = ChunkingConfig(
            chunkSize = ingestionProperties.processing.chunkSize,
            chunkOverlap = ingestionProperties.processing.chunkOverlap
        )
        return chunkingService.chunk(content, mimeType, config, sourceId)
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
