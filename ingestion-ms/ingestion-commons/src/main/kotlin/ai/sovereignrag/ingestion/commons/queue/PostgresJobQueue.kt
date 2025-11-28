package ai.sovereignrag.ingestion.commons.queue

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class PostgresJobQueue(
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties
) : JobQueue {

    override fun enqueue(job: IngestionJob) {
        job.status = JobStatus.QUEUED
        job.visibleAfter = null
        jobRepository.save(job)
        log.info { "Enqueued job ${job.id} with priority ${job.priority}" }
    }

    override fun dequeue(workerId: String): IngestionJob? {
        val now = Instant.now()

        val queuedJobs = jobRepository.findQueuedJobsByPriority(now, PageRequest.of(0, 1))

        if (queuedJobs.isEmpty) {
            return null
        }

        val job = queuedJobs.content.first()

        job.status = JobStatus.PROCESSING
        job.lockedAt = now
        job.lockedBy = workerId
        job.startedAt = now

        val savedJob = jobRepository.save(job)
        log.info { "Worker $workerId claimed job ${savedJob.id}" }

        return savedJob
    }

    override fun complete(jobId: UUID) {
        jobRepository.findById(jobId).ifPresent { job ->
            job.markCompleted()
            jobRepository.save(job)
            log.info { "Job $jobId completed" }
        }
    }

    override fun fail(jobId: UUID, errorMessage: String) {
        jobRepository.findById(jobId).ifPresent { job ->
            job.markFailed(errorMessage)
            jobRepository.save(job)
            log.warn { "Job $jobId failed: $errorMessage" }
        }
    }

    override fun retry(jobId: UUID) {
        jobRepository.findById(jobId).ifPresent { job ->
            if (job.canRetry()) {
                job.incrementRetry()
                job.status = JobStatus.QUEUED
                job.lockedAt = null
                job.lockedBy = null
                job.visibleAfter = Instant.now().plusSeconds(
                    calculateBackoffSeconds(job.retryCount)
                )
                jobRepository.save(job)
                log.info { "Job $jobId scheduled for retry ${job.retryCount}" }
            } else {
                job.markFailed("Max retries exceeded")
                jobRepository.save(job)
                log.warn { "Job $jobId exceeded max retries" }
            }
        }
    }

    override fun releaseStaleJobs() {
        val staleThreshold = Instant.now().minus(
            ingestionProperties.queue.lockTimeoutMinutes, ChronoUnit.MINUTES
        )
        val visibleAfter = Instant.now().plus(
            ingestionProperties.queue.visibilityTimeoutMinutes, ChronoUnit.MINUTES
        )

        val released = jobRepository.releaseStaleJobs(staleThreshold, visibleAfter)
        if (released > 0) {
            log.warn { "Released $released stale jobs" }
        }
    }

    override fun getQueueDepth(): Long {
        return jobRepository.findQueuedJobsByPriority(
            Instant.now(),
            PageRequest.of(0, Int.MAX_VALUE)
        ).totalElements
    }

    override fun getQueueDepthByPriority(): Map<Int, Long> {
        val jobs = jobRepository.findQueuedJobsByPriority(
            Instant.now(),
            PageRequest.of(0, 1000)
        )
        return jobs.content.groupingBy { it.priority }.eachCount().mapValues { it.value.toLong() }
    }

    private fun calculateBackoffSeconds(retryCount: Int): Long {
        return (Math.pow(2.0, retryCount.toDouble()) * 30).toLong().coerceAtMost(3600)
    }
}
