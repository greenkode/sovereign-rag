package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.core.processor.JobProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class JobPollerService(
    private val jobQueue: JobQueue,
    private val processors: List<JobProcessor>,
    private val ingestionProperties: IngestionProperties
) {
    private val workerId = "${InetAddress.getLocalHost().hostName}-${UUID.randomUUID().toString().take(8)}"

    fun pollAndProcessBatch(batchSize: Int = ingestionProperties.queue.batchSize) {
        repeat(batchSize) {
            val job = jobQueue.dequeue(workerId) ?: return@repeat

            log.info { "Worker $workerId processing job ${job.id} (${job.jobType})" }

            try {
                val processor = findProcessor(job.jobType)
                processor.process(job)
                jobQueue.complete(job.id!!)
                log.info { "Job ${job.id} completed successfully" }
            } catch (e: Exception) {
                log.error(e) { "Job ${job.id} failed: ${e.message}" }
                jobQueue.fail(job.id!!, e.message ?: "Unknown error")

                job.canRetry().takeIf { it }?.let {
                    jobQueue.retry(job.id!!)
                }
            }
        }
    }

    private fun findProcessor(jobType: JobType): JobProcessor =
        processors.find { it.supports(jobType) }
            ?: throw IllegalStateException("No processor found for job type: $jobType")

    fun releaseStaleJobs() {
        log.debug { "Checking for stale jobs..." }
        jobQueue.releaseStaleJobs()
    }

    fun getWorkerId(): String = workerId
}
