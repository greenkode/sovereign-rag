package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.springframework.stereotype.Component
import java.io.InputStream

private val log = KotlinLogging.logger {}

@Component
class FileProcessor(
    private val fileUploadGateway: FileUploadGateway,
    private val jobRepository: IngestionJobRepository,
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

        job.markCompleted(
            chunksCreated = chunks.size,
            bytesProcessed = content.length.toLong()
        )
        jobRepository.save(job)
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
