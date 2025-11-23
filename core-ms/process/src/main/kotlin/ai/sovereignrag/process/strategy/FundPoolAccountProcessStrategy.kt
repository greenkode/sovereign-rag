package ai.sovereignrag.process.strategy

import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessStrategyBeanNames
import ai.sovereignrag.process.service.FundPoolAccountProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component(ProcessStrategyBeanNames.FUND_POOL_ACCOUNT_PROCESS_STRATEGY)
class FundPoolAccountProcessStrategy(
    @Lazy private val fundPoolAccountProcessService: FundPoolAccountProcessService
) : ProcessStrategy {

    private val log = KotlinLogging.logger {}

    override fun processEvent(
        process: ProcessDto,
        event: ProcessEvent
    ): ProcessState {

        log.info { "Processing fund pool account event: $event for process: ${process.publicId}" }

        return when (process.state to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED,
            ProcessState.COMPLETE to ProcessEvent.REVERSE_TRANSACTION,
            ProcessState.EXPIRED to ProcessEvent.REVERSE_TRANSACTION -> {
                fundPoolAccountProcessService.completeFundPoolAccount(process)
                ProcessState.COMPLETE
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> {
                fundPoolAccountProcessService.handleFundPoolAccountFailure(process)
                ProcessState.FAILED
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> {
                fundPoolAccountProcessService.handleFundPoolAccountExpiry(process)
                ProcessState.EXPIRED
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED,
            ProcessState.PENDING to ProcessEvent.REVERSE_TRANSACTION,
            ProcessState.COMPLETE to ProcessEvent.REVERSE_TRANSACTION,
            ProcessState.EXPIRED to ProcessEvent.REVERSE_TRANSACTION -> {
                fundPoolAccountProcessService.reversePendingTransaction(process)
                ProcessState.FAILED
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
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_COMPLETED, ProcessState.COMPLETE) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_FAILED, ProcessState.FAILED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_EXPIRED, ProcessState.EXPIRED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.REVERSE_TRANSACTION, ProcessState.PENDING) -> true
            Triple(ProcessState.COMPLETE, ProcessEvent.REVERSE_TRANSACTION, ProcessState.COMPLETE) -> true
            Triple(ProcessState.EXPIRED, ProcessEvent.REVERSE_TRANSACTION, ProcessState.EXPIRED) -> true
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
            else -> currentState
        }
    }
}