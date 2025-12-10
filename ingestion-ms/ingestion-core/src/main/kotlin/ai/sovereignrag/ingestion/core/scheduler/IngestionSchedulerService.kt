package ai.sovereignrag.ingestion.core.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
class IngestionSchedulerService(
    private val scheduler: Scheduler
) {

    fun scheduleJob(payload: IngestionScheduleJobPayload) {
        val jobKey = JobKey.jobKey(payload.reference, payload.group)

        if (scheduler.checkExists(jobKey)) {
            log.info { "Job ${payload.reference} already exists in group ${payload.group}, skipping" }
            return
        }

        val jobDetail = buildJobDetail(payload)
        val trigger = buildJobTrigger(jobDetail, payload)

        scheduler.scheduleJob(jobDetail, trigger)
        log.info { "Scheduled job: ${payload.reference} in group: ${payload.group}" }
    }

    private fun buildJobDetail(payload: IngestionScheduleJobPayload): JobDetail {
        val jobDataMap = JobDataMap(payload.data)

        return JobBuilder.newJob(payload.jobType)
            .withIdentity(payload.reference, payload.group)
            .withDescription(payload.description)
            .usingJobData(jobDataMap)
            .storeDurably()
            .build()
    }

    private fun buildJobTrigger(jobDetail: JobDetail, payload: IngestionScheduleJobPayload): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity(jobDetail.key.name, payload.group)
            .withDescription(payload.description)
            .startAt(Date.from(payload.startAt))
            .apply {
                payload.endAt?.let {
                    endAt(Date.from(it))
                }
            }
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .apply {
                        payload.repeatIntervalInSeconds?.let { withIntervalInSeconds(it) }
                        if (payload.repeatForever) {
                            repeatForever()
                        } else {
                            withRepeatCount(payload.repeatCount)
                        }
                    }
                    .withMisfireHandlingInstructionFireNow()
            ).build()
    }

    fun deleteJob(reference: String, group: String): Boolean {
        return try {
            val jobKey = JobKey.jobKey(reference, group)
            scheduler.deleteJob(jobKey)
        } catch (e: SchedulerException) {
            log.error(e) { "Failed to delete job: $reference in group: $group" }
            false
        }
    }

    fun jobExists(reference: String, group: String): Boolean {
        return try {
            val jobKey = JobKey.jobKey(reference, group)
            scheduler.checkExists(jobKey)
        } catch (e: SchedulerException) {
            log.error(e) { "Failed to check job existence: $reference in group: $group" }
            false
        }
    }
}

data class IngestionScheduleJobPayload(
    val reference: String,
    val group: String,
    val description: String,
    val startAt: Instant,
    val jobType: Class<out Job>,
    val data: Map<String, String> = emptyMap(),
    val repeatForever: Boolean = false,
    val repeatIntervalInSeconds: Int? = null,
    val repeatCount: Int = 0,
    val endAt: Instant? = null
)
