package ai.sovereignrag.ingestion.core.service

import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.audit.IngestionAuditEventPublisher
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
    private val ingestionProperties: IngestionProperties,
    private val auditEventPublisher: IngestionAuditEventPublisher,
    private val jobRepository: IngestionJobRepository
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

                jobRepository.findById(job.id!!).ifPresent { completedJob ->
                    publishCompletedEvent(completedJob)
                }
            } catch (e: Exception) {
                log.error(e) { "Job ${job.id} failed: ${e.message}" }
                jobQueue.fail(job.id!!, e.message ?: "Unknown error")

                jobRepository.findById(job.id!!).ifPresent { failedJob ->
                    publishFailedEvent(failedJob, e.message)
                }

                job.canRetry().takeIf { it }?.let {
                    jobQueue.retry(job.id!!)
                }
            }
        }
    }

    private fun publishCompletedEvent(job: IngestionJob) {
        getCompletedEventType(job.jobType)?.let { eventType ->
            auditEventPublisher.publishJobCompleted(job, eventType)
        }
    }

    private fun publishFailedEvent(job: IngestionJob, errorMessage: String?) {
        getFailedEventType(job.jobType)?.let { eventType ->
            auditEventPublisher.publishJobFailed(job, eventType, errorMessage)
        }
    }

    private fun getCompletedEventType(jobType: JobType): IngestionEventType? =
        when (jobType) {
            JobType.FILE_UPLOAD -> IngestionEventType.FILE_UPLOAD_COMPLETED
            JobType.BATCH_IMPORT -> IngestionEventType.BATCH_UPLOAD_COMPLETED
            JobType.FOLDER_IMPORT -> IngestionEventType.FOLDER_UPLOAD_COMPLETED
            JobType.WEB_SCRAPE -> IngestionEventType.WEB_SCRAPE_COMPLETED
            JobType.TEXT_INPUT -> IngestionEventType.TEXT_INPUT_COMPLETED
            JobType.QA_IMPORT -> IngestionEventType.QA_PAIRS_COMPLETED
            JobType.RSS_FEED -> IngestionEventType.RSS_FEED_COMPLETED
            JobType.EMBEDDING -> IngestionEventType.EMBEDDING_COMPLETED
        }

    private fun getFailedEventType(jobType: JobType): IngestionEventType? =
        when (jobType) {
            JobType.FILE_UPLOAD -> IngestionEventType.FILE_UPLOAD_FAILED
            JobType.BATCH_IMPORT -> IngestionEventType.BATCH_UPLOAD_FAILED
            JobType.FOLDER_IMPORT -> IngestionEventType.FOLDER_UPLOAD_FAILED
            JobType.WEB_SCRAPE -> IngestionEventType.WEB_SCRAPE_FAILED
            JobType.TEXT_INPUT -> IngestionEventType.TEXT_INPUT_FAILED
            JobType.QA_IMPORT -> IngestionEventType.QA_PAIRS_FAILED
            JobType.RSS_FEED -> IngestionEventType.RSS_FEED_FAILED
            JobType.EMBEDDING -> IngestionEventType.EMBEDDING_FAILED
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
