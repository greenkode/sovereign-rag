package ai.sovereignrag.ingestion.core.config

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.core.job.IngestionJobPollerQuartzJob
import ai.sovereignrag.ingestion.core.job.StaleJobReleaserQuartzJob
import ai.sovereignrag.ingestion.core.scheduler.IngestionScheduleJobPayload
import ai.sovereignrag.ingestion.core.scheduler.IngestionSchedulerService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import java.time.Instant

private val log = KotlinLogging.logger {}

@Configuration
class IngestionSchedulerConfig(
    private val schedulerService: IngestionSchedulerService,
    private val ingestionProperties: IngestionProperties
) {

    companion object {
        const val JOB_POLLER_GROUP = "ingestion-poller"
        const val JOB_POLLER_REFERENCE = "job-poller"
        const val STALE_RELEASER_REFERENCE = "stale-releaser"
    }

    @PostConstruct
    fun scheduleIngestionJobs() {
        scheduleJobPoller()
        scheduleStaleJobReleaser()
        log.info { "Ingestion scheduler jobs configured" }
    }

    private fun scheduleJobPoller() {
        val pollIntervalSeconds = (ingestionProperties.queue.pollIntervalMs / 1000).toInt()

        schedulerService.scheduleJob(
            IngestionScheduleJobPayload(
                jobType = IngestionJobPollerQuartzJob::class.java,
                reference = JOB_POLLER_REFERENCE,
                group = JOB_POLLER_GROUP,
                description = "Polls and processes ingestion jobs",
                startAt = Instant.now(),
                repeatIntervalInSeconds = pollIntervalSeconds.coerceAtLeast(1),
                repeatForever = true,
                data = mapOf("batchSize" to ingestionProperties.queue.batchSize.toString())
            )
        )

        log.info { "Scheduled job poller with ${pollIntervalSeconds}s interval" }
    }

    private fun scheduleStaleJobReleaser() {
        val releaseIntervalSeconds = (ingestionProperties.queue.lockTimeoutMinutes * 60).toInt()

        schedulerService.scheduleJob(
            IngestionScheduleJobPayload(
                jobType = StaleJobReleaserQuartzJob::class.java,
                reference = STALE_RELEASER_REFERENCE,
                group = JOB_POLLER_GROUP,
                description = "Releases stale locked jobs",
                startAt = Instant.now().plusSeconds(60),
                repeatIntervalInSeconds = releaseIntervalSeconds,
                repeatForever = true,
                data = emptyMap()
            )
        )

        log.info { "Scheduled stale job releaser with ${releaseIntervalSeconds}s interval" }
    }
}
