package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BatchImportProcessor(
    private val jobRepository: IngestionJobRepository
) : JobProcessor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.BATCH_IMPORT

    override fun process(job: IngestionJob) {
        log.info { "Checking batch import job ${job.id} status" }

        val childJobs = jobRepository.findByParentJobId(job.id!!)

        if (childJobs.isEmpty()) {
            job.markFailed("No child jobs found for batch import")
            jobRepository.save(job)
            return
        }

        val totalJobs = childJobs.size
        val completedJobs = childJobs.count { it.status == JobStatus.COMPLETED }
        val failedJobs = childJobs.count { it.status == JobStatus.FAILED }
        val pendingJobs = totalJobs - completedJobs - failedJobs

        val progress = ((completedJobs + failedJobs) * 100) / totalJobs
        job.updateProgress(progress)

        val totalChunks = childJobs.sumOf { it.chunksCreated }
        val totalBytes = childJobs.sumOf { it.bytesProcessed }

        when {
            pendingJobs > 0 -> {
                log.info { "Batch ${job.id}: $completedJobs/$totalJobs completed, $pendingJobs pending" }
                jobRepository.save(job)
            }
            failedJobs == totalJobs -> {
                job.markFailed("All child jobs failed")
                jobRepository.save(job)
            }
            failedJobs > 0 -> {
                job.markCompleted(
                    chunksCreated = totalChunks,
                    bytesProcessed = totalBytes
                )
                job.errorMessage = "$failedJobs of $totalJobs files failed to process"
                jobRepository.save(job)
                log.warn { "Batch ${job.id} completed with $failedJobs failures" }
            }
            else -> {
                job.markCompleted(
                    chunksCreated = totalChunks,
                    bytesProcessed = totalBytes
                )
                jobRepository.save(job)
                log.info { "Batch ${job.id} completed: $totalJobs files processed, $totalChunks chunks created" }
            }
        }
    }
}
