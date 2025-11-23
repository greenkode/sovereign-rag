package ai.sovereignrag.process.domain.model

import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import java.util.UUID

/**
 * Event published when a process state changes
 */
data class ProcessStateChangedEvent(
    val processId: UUID,
    val processType: ProcessType,
    val oldState: ProcessState,
    val newState: ProcessState,
    val userId: UUID,
    val timestamp: Long = System.currentTimeMillis()
) {
    
    fun isCompleted(): Boolean = newState == ProcessState.COMPLETE
    fun isFailed(): Boolean = newState == ProcessState.FAILED
    fun isExpired(): Boolean = newState == ProcessState.EXPIRED
}