package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.core.processor.FileProcessor
import ai.sovereignrag.ingestion.core.processor.WebScrapeProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

@Service
class JobPollerService(
    private val jobQueue: JobQueue,
    private val fileProcessor: FileProcessor,
    private val webScrapeProcessor: WebScrapeProcessor,
    private val ingestionProperties: IngestionProperties
) {
    private val workerId = "${InetAddress.getLocalHost().hostName}-${UUID.randomUUID().toString().take(8)}"
    private val processing = AtomicBoolean(false)

    @Scheduled(fixedDelayString = "\${ingestion.queue.poll-interval-ms:1000}")
    fun pollForJobs() {
        if (!processing.compareAndSet(false, true)) {
            return
        }

        try {
            repeat(ingestionProperties.queue.batchSize) {
                val job = jobQueue.dequeue(workerId) ?: return@repeat

                log.info { "Worker $workerId processing job ${job.id} (${job.jobType})" }

                try {
                    when (job.jobType) {
                        JobType.FILE_UPLOAD -> fileProcessor.process(job)
                        JobType.WEB_SCRAPE -> webScrapeProcessor.process(job)
                        JobType.BATCH_IMPORT -> fileProcessor.process(job)
                        JobType.FOLDER_IMPORT -> fileProcessor.process(job)
                    }
                    jobQueue.complete(job.id!!)
                    log.info { "Job ${job.id} completed successfully" }
                } catch (e: Exception) {
                    log.error(e) { "Job ${job.id} failed: ${e.message}" }
                    jobQueue.fail(job.id!!, e.message ?: "Unknown error")

                    if (job.canRetry()) {
                        jobQueue.retry(job.id!!)
                    }
                }
            }
        } finally {
            processing.set(false)
        }
    }

    @Scheduled(fixedDelayString = "\${ingestion.queue.lock-timeout-minutes:30}000")
    fun releaseStaleJobs() {
        log.debug { "Checking for stale jobs..." }
        jobQueue.releaseStaleJobs()
    }
}
