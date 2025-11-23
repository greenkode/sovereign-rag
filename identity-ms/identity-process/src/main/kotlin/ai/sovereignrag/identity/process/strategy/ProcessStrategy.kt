package ai.sovereignrag.identity.process.strategy

import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState


/**
 * Strategy interface for handling different process types
 */
interface ProcessStrategy {
    
    /**
     * Processes an event for the given process and returns the new state
     */
    fun processEvent(
        process: ProcessDto,
        event: ProcessEvent
    ): ProcessState
    
    /**
     * Validates if the state transition is allowed for this process type
     */
    fun isValidTransition(
        currentState: ProcessState,
        event: ProcessEvent,
        targetState: ProcessState
    ): Boolean
    
    /**
     * Calculates the expected state for a given current state and event
     */
    fun calculateExpectedState(
        currentState: ProcessState,
        event: ProcessEvent
    ): ProcessState
}