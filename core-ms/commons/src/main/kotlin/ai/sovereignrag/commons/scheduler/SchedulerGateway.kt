package ai.sovereignrag.commons.scheduler

import org.quartz.JobExecutionContext

interface SchedulerGateway {

    companion object {
        const val MAX_RETRY = "MAX_RETRY"
        const val RETRY_COUNT = "RETRY_COUNT"
    }

    fun scheduleJob(payload: ScheduleJobPayload)

    fun deleteJob(context: JobExecutionContext)
    
    fun deleteJobById(reference: String, group: String): Boolean
    
    fun rescheduleJob(context: JobExecutionContext, delayInSeconds: Long, reference: String)
}