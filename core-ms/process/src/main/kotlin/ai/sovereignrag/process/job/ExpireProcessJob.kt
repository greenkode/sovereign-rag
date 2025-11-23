package ai.sovereignrag.process.job

import ai.sovereignrag.commons.enumeration.Channel
import ai.sovereignrag.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessHeader.PROCESS_ID
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.user.UserGateway
import ai.sovereignrag.process.spi.ProcessService
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ExpireProcessJob(
    val processService: ProcessService,
    private val userGateway: UserGateway
) : QuartzJobBean() {

    @Transactional
    override fun executeInternal(context: JobExecutionContext) {

        val processId = UUID.fromString(context.mergedJobDataMap[PROCESS_ID.name] as String)

        processService.findPendingProcessByPublicId(processId).takeIf { it?.state == ProcessState.PENDING } ?.let {
            processService.makeRequest(
                MakeProcessRequestPayload(
                    userGateway.getSystemUserId(),
                    processId,
                    ProcessEvent.PROCESS_EXPIRED,
                    ProcessRequestType.EXPIRE_PROCESS,
                    Channel.SYSTEM,
                    ProcessState.COMPLETE
                )
            )
        }
    }
}