package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
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
    private val ingestionProperties: IngestionProperties
) {
    private val tika = Tika()

    fun process(job: IngestionJob) {
        log.info { "Processing file job ${job.id}: ${job.fileName}" }

        val sourceKey = job.sourceReference
            ?: throw IllegalStateException("No source reference for job ${job.id}")

        updateProgress(job, 10)

        val inputStream = fileUploadGateway.getFileStream(sourceKey)

        updateProgress(job, 30)

        val content = extractContent(inputStream, job.mimeType)
        updateProgress(job, 60)

        val chunks = chunkContent(content)
        updateProgress(job, 80)

        log.info { "Extracted ${chunks.size} chunks from ${job.fileName}" }

        job.knowledgeBaseId?.let { kbId ->
            val knowledgeSourceId = job.knowledgeSourceId ?: UUID.randomUUID()
            createEmbeddingJob(job, chunks, knowledgeSourceId)
            log.info { "Created embedding job for ${chunks.size} chunks" }
        }

        job.markCompleted(
            chunksCreated = chunks.size,
            bytesProcessed = content.length.toLong()
        )
        jobRepository.save(job)
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID
    ) {
        val chunkData = ChunkJobData(
            chunks = chunks.mapIndexed { index, content -> ChunkInfo(index, content) },
            sourceType = "FILE",
            fileName = parentJob.fileName,
            sourceUrl = null,
            title = parentJob.fileName
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

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
