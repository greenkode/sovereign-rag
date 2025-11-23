package ai.sovereignrag.process.listener

import ai.sovereignrag.commons.process.enumeration.ProcessHeader.PROCESS_ID
import ai.sovereignrag.commons.scheduler.ScheduleJobPayload
import ai.sovereignrag.process.domain.model.ProcessCreatedEvent
import ai.sovereignrag.process.job.ExpireProcessJob
import ai.sovereignrag.scheduler.SchedulerService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProcessExpiryScheduler(val schedulerService: SchedulerService) {

    val log = KotlinLogging.logger {}

    @ApplicationModuleListener
    fun on(event: ProcessCreatedEvent) {

        log.info { "Schedule Expiry Task for process: ${event.id}" }

        if (event.processType.timeInSeconds > -1L) {
            schedulerService.scheduleJob(
                ScheduleJobPayload(
                    event.id.toString(),
                    "process-expiry",
                    "Expire Process Job",
                    Instant.now().plusSeconds(event.expiry ?: event.processType.timeInSeconds),
                    ExpireProcessJob::class.java,
                    mapOf(PROCESS_ID.name to event.id.toString())
                )
            )
        }
    }
}
