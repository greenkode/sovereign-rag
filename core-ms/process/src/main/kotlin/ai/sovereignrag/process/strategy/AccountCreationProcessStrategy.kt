package ai.sovereignrag.process.strategy

import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessStrategyBeanNames
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.process.service.AccountCreationProcessService
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component(ProcessStrategyBeanNames.ACCOUNT_CREATION_PROCESS_STRATEGY)
class AccountCreationProcessStrategy(
    @Lazy private val accountCreationProcessService: AccountCreationProcessService,
) : ProcessStrategy {
    
    private val log = KotlinLogging.logger {}
    
    override fun processEvent(
        process: ProcessDto, 
        event: ProcessEvent
    ): ProcessState {
        
        log.info { "Processing account creation event: $event for process: ${process.publicId}" }
        
        return when (process.state to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> {
                accountCreationProcessService.completeAccountCreation(process)
                ProcessState.COMPLETE
            }
            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> {
                accountCreationProcessService.handleAccountCreationFailure(process)
                ProcessState.FAILED
            }
            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> {
                accountCreationProcessService.handleAccountCreationExpiry(process)
                ProcessState.EXPIRED
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
            else -> currentState // No state change expected
        }
    }
}