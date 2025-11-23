package ai.sovereignrag.scheduler

import ai.sovereignrag.commons.scheduler.ScheduleJobPayload
import ai.sovereignrag.commons.scheduler.SchedulerGateway
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.stereotype.Service
import java.util.Date


@Service
class SchedulerService(val scheduler: Scheduler) : SchedulerGateway {

    override fun scheduleJob(payload: ScheduleJobPayload) {

        val jobDetail = buildJobDetail(payload)

        val trigger = buildJobTrigger(jobDetail, payload)

        scheduler.scheduleJob(jobDetail, trigger)
    }

    private fun buildJobDetail(payload: ScheduleJobPayload): JobDetail {

        val jobDataMap = JobDataMap(payload.data)

        return JobBuilder.newJob(payload.jobType)
            .withIdentity(payload.reference, payload.group)
            .withDescription(payload.description)
            .usingJobData(jobDataMap)
            .storeDurably()
            .build()
    }

    private fun buildJobTrigger(jobDetail: JobDetail, payload: ScheduleJobPayload): Trigger {
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

    override fun deleteJob(context: JobExecutionContext) {
        try {
            context.scheduler.deleteJob(context.jobDetail.key)
        } catch (e: SchedulerException) {
            throw JobSchedulingException(e.message, e)
        }
    }
    
    override fun deleteJobById(reference: String, group: String): Boolean {
        return try {
            val jobKey = JobKey.jobKey(reference, group)
            scheduler.deleteJob(jobKey)
        } catch (e: SchedulerException) {
            throw JobSchedulingException(e.message, e)
        }
    }
    
    override fun rescheduleJob(context: JobExecutionContext, delayInSeconds: Long, reference: String) {
        try {
            val newTrigger = TriggerBuilder.newTrigger()
                .forJob(context.jobDetail)
                .withIdentity("${context.trigger.key.name}-retry", context.trigger.key.group)
                .startAt(Date(System.currentTimeMillis() + delayInSeconds * 1000))
                .build()
            
            context.scheduler.rescheduleJob(context.trigger.key, newTrigger)
        } catch (e: SchedulerException) {
            throw JobSchedulingException(e.message, e)
        }
    }
}