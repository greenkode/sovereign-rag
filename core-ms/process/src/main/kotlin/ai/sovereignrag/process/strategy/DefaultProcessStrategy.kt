package ai.sovereignrag.process.strategy

import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessStrategyBeanNames
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component(ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY)
class DefaultProcessStrategy : ProcessStrategy {

    private val log = KotlinLogging.logger {}

    override fun processEvent(
        process: ProcessDto,
        event: ProcessEvent
    ): ProcessState {

        log.info { "Processing Process event: $event for process: ${process.publicId}" }

        return when (process.state to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> {
                log.info { "Default operation completed for process: ${process.publicId}" }
                ProcessState.COMPLETE
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> {
                log.info { "Default operation failed for process: ${process.publicId}" }
                ProcessState.FAILED
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> {
                log.info { "Default operation expired for process: ${process.publicId}" }
                ProcessState.EXPIRED
            }

            ProcessState.PENDING to ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED,
            ProcessState.PENDING to ProcessEvent.CHAT_MESSAGE_RECEIVED,
                 ProcessState.PENDING to ProcessEvent.SUPPORT_TICKET_CREATED -> {
                log.info { "Default operation verified for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            else -> {
                log.warn { "Invalid state transition: ${process.state} -> $event" }
                throw IllegalStateException("Invalid transition from ${process.state} with event $event")
            }
        }
    }

    override fun isValidTransition(
        currentState: ProcessState,
        event: ProcessEvent,
        targetState: ProcessState
    ): Boolean {
        return when (Triple(currentState, event, targetState)) {
            Triple(ProcessState.PENDING, ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_COMPLETED, ProcessState.COMPLETE) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_FAILED, ProcessState.FAILED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_EXPIRED, ProcessState.EXPIRED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.CHAT_MESSAGE_RECEIVED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.SUPPORT_TICKET_CREATED, ProcessState.PENDING) -> true
            else -> false
        }
    }

    override fun calculateExpectedState(
        currentState: ProcessState,
        event: ProcessEvent
    ): ProcessState {
        return when (currentState to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> ProcessState.COMPLETE
            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> ProcessState.FAILED
            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> ProcessState.EXPIRED
            ProcessState.PENDING to ProcessEvent.CHAT_MESSAGE_RECEIVED -> ProcessState.PENDING
            ProcessState.PENDING to ProcessEvent.SUPPORT_TICKET_CREATED -> ProcessState.PENDING
            else -> currentState // No state change expected
        }
    }
}