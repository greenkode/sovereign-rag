package ai.sovereignrag.ingestion.core.job

import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.core.processor.EmbeddingProcessor
import ai.sovereignrag.ingestion.core.processor.FileProcessor
import ai.sovereignrag.ingestion.core.processor.WebScrapeProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@DisallowConcurrentExecution
class IngestionJobPollerQuartzJob(
    private val jobQueue: JobQueue,
    private val fileProcessor: FileProcessor,
    private val webScrapeProcessor: WebScrapeProcessor,
    private val embeddingProcessor: EmbeddingProcessor
) : Job {

    private val workerId = "${InetAddress.getLocalHost().hostName}-${UUID.randomUUID().toString().take(8)}"

    override fun execute(context: JobExecutionContext) {
        val batchSize = context.mergedJobDataMap.getString("batchSize")?.toIntOrNull()?.takeIf { it > 0 } ?: 10

        repeat(batchSize) {
            val job = jobQueue.dequeue(workerId) ?: return@repeat

            log.info { "Worker $workerId processing job ${job.id} (${job.jobType})" }

            try {
                when (job.jobType) {
                    JobType.FILE_UPLOAD -> fileProcessor.process(job)
                    JobType.WEB_SCRAPE -> webScrapeProcessor.process(job)
                    JobType.BATCH_IMPORT -> fileProcessor.process(job)
                    JobType.FOLDER_IMPORT -> fileProcessor.process(job)
                    JobType.EMBEDDING -> embeddingProcessor.process(job)
                }
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
}

@Component
@DisallowConcurrentExecution
class StaleJobReleaserQuartzJob(
    private val jobQueue: JobQueue
) : Job {

    override fun execute(context: JobExecutionContext) {
        log.debug { "Checking for stale jobs..." }
        jobQueue.releaseStaleJobs()
    }
}
