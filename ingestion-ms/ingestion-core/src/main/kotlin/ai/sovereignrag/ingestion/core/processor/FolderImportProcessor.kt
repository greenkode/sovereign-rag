package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.entity.SourceType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.command.FolderImportJobMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.Tika
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

private val log = KotlinLogging.logger {}

@Component
class FolderImportProcessor(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val fileUploadGateway: FileUploadGateway,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper
) : JobProcessor {

    private val tika = Tika()

    override fun supports(jobType: JobType): Boolean = jobType == JobType.FOLDER_IMPORT

    override fun process(job: IngestionJob) {
        log.info { "Processing folder import job ${job.id}" }

        val metadata = parseMetadata(job)
        val s3Key = job.sourceReference
            ?: throw IllegalStateException("Source reference (S3 key) is required for folder import job ${job.id}")

        updateProgress(job, 10)

        val tempDir = Files.createTempDirectory("folder-import-${job.id}").toFile()
        var extractedFiles: List<ExtractedFile> = emptyList()

        try {
            log.info { "Downloading ZIP from S3: $s3Key" }
            val zipBytes = fileUploadGateway.getFileStream(s3Key).use { it.readBytes() }
            updateProgress(job, 30)

            log.info { "Extracting ZIP contents" }
            extractedFiles = extractZip(zipBytes, tempDir, metadata.preserveStructure)
            updateProgress(job, 50)

            log.info { "Extracted ${extractedFiles.size} files from ZIP" }

            val supportedFiles = extractedFiles.filter { file ->
                file.mimeType in ingestionProperties.processing.supportedMimeTypes
            }

            log.info { "Found ${supportedFiles.size} supported files out of ${extractedFiles.size} total" }

            if (supportedFiles.isEmpty()) {
                job.markFailed("No supported files found in ZIP archive")
                jobRepository.save(job)
                return
            }

            updateProgress(job, 60)

            var createdJobs = 0
            supportedFiles.forEach { extractedFile ->
                val s3FileKey = uploadExtractedFile(extractedFile, job)

                val childJob = IngestionJob(
                    organizationId = job.organizationId,
                    jobType = JobType.FILE_UPLOAD,
                    knowledgeBaseId = job.knowledgeBaseId,
                    priority = job.priority
                ).apply {
                    parentJobId = job.id
                    sourceType = SourceType.S3_KEY
                    sourceReference = s3FileKey
                    fileName = extractedFile.relativePath
                    fileSize = extractedFile.size
                    mimeType = extractedFile.mimeType
                    this.metadata = if (metadata.preserveStructure) {
                        objectMapper.writeValueAsString(mapOf("folderPath" to extractedFile.folderPath))
                    } else null
                }

                val savedChildJob = jobRepository.save(childJob)
                jobQueue.enqueue(savedChildJob)
                createdJobs++
            }

            updateProgress(job, 90)

            job.status = JobStatus.PROCESSING
            job.chunksCreated = createdJobs
            job.bytesProcessed = supportedFiles.sumOf { it.size }
            jobRepository.save(job)

            log.info { "Created $createdJobs child jobs for folder import ${job.id}" }

        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun parseMetadata(job: IngestionJob): FolderImportJobMetadata {
        val metadata = job.metadata
            ?: return FolderImportJobMetadata(preserveStructure = true)

        return objectMapper.readValue<FolderImportJobMetadata>(metadata)
    }

    private fun extractZip(zipBytes: ByteArray, tempDir: File, preserveStructure: Boolean): List<ExtractedFile> {
        val extractedFiles = mutableListOf<ExtractedFile>()

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val fileName = entry.name
                    val sanitizedName = sanitizeFileName(fileName)

                    if (sanitizedName.isNotBlank() && !isHiddenOrSystemFile(fileName)) {
                        val targetFile = File(tempDir, sanitizedName)
                        targetFile.parentFile?.mkdirs()

                        targetFile.outputStream().use { output ->
                            zis.copyTo(output)
                        }

                        val mimeType = tika.detect(targetFile)
                        val folderPath = if (preserveStructure) {
                            fileName.substringBeforeLast("/", "")
                        } else ""

                        extractedFiles.add(
                            ExtractedFile(
                                file = targetFile,
                                relativePath = fileName,
                                folderPath = folderPath,
                                size = targetFile.length(),
                                mimeType = mimeType
                            )
                        )
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return extractedFiles
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace("\\", "/")
            .split("/")
            .filter { it.isNotBlank() && it != ".." && it != "." }
            .joinToString("/")
    }

    private fun isHiddenOrSystemFile(fileName: String): Boolean {
        val name = fileName.substringAfterLast("/")
        return name.startsWith(".") ||
            name.startsWith("__MACOSX") ||
            name == "Thumbs.db" ||
            name == ".DS_Store"
    }

    private fun uploadExtractedFile(extractedFile: ExtractedFile, parentJob: IngestionJob): String {
        val result = fileUploadGateway.uploadFile(
            inputStream = extractedFile.file.inputStream(),
            fileName = extractedFile.relativePath,
            contentType = extractedFile.mimeType,
            size = extractedFile.size,
            category = "${ingestionProperties.storage.uploadsPrefix.trimEnd('/')}/${parentJob.id}",
            ownerId = parentJob.organizationId
        )
        return result.key
    }

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }

    private data class ExtractedFile(
        val file: File,
        val relativePath: String,
        val folderPath: String,
        val size: Long,
        val mimeType: String
    )
}
