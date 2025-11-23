package ai.sovereignrag.process.listener

import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.process.domain.model.ProcessStateChangedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Listens to process state change events and handles side effects
 */
@Component
class ProcessStateChangeEventListener {
    
    private val log = KotlinLogging.logger {}
    
    @Async
    @EventListener
    fun handleProcessStateChange(event: ProcessStateChangedEvent) {
        log.info { 
            "Process state changed: ${event.processId} from ${event.oldState} to ${event.newState}" 
        }
        
        try {
            // Send notifications based on state change
            sendNotifications(event)
            
            // Handle specific state changes
            when {
                event.isCompleted() -> handleProcessCompletion(event)
                event.isFailed() -> handleProcessFailure(event)
                event.isExpired() -> handleProcessExpiry(event)
            }
            
        } catch (e: Exception) {
            log.error(e) { "Error handling process state change event: $event" }
        }
    }
    
    private fun publishAuditEvent(event: ProcessStateChangedEvent) {
        log.info { "Publishing audit event for process state change: ${event.processId}" }
        // TODO: Implement audit event publishing
        // This would integrate with the existing audit system
    }
    
    private fun sendNotifications(event: ProcessStateChangedEvent) {
        // TODO: Implement notification logic based on process type and state
        when (event.newState) {
            ProcessState.COMPLETE -> {
                log.info { "Sending completion notification for process ${event.processId}" }
                // TODO: Integrate with notification gateway
            }
            ProcessState.FAILED -> {
                log.info { "Sending failure notification for process ${event.processId}" }
                // TODO: Integrate with notification gateway
            }
            ProcessState.EXPIRED -> {
                log.info { "Sending expiry notification for process ${event.processId}" }
                // TODO: Integrate with notification gateway
            }
            else -> {
                // No notification needed for other states
            }
        }
    }
    
    private fun handleProcessCompletion(event: ProcessStateChangedEvent) {
        log.info { "Handling completion for process ${event.processId} of type ${event.processType}" }
        
        // TODO: Implement completion-specific logic
        // - Update related entities
        // - Trigger follow-up processes
        // - Clean up resources
    }
    
    private fun handleProcessFailure(event: ProcessStateChangedEvent) {
        log.info { "Handling failure for process ${event.processId} of type ${event.processType}" }
        
        // TODO: Implement failure-specific logic
        // - Trigger compensation actions
        // - Clean up resources
        // - Log failure details for analysis
    }
    
    private fun handleProcessExpiry(event: ProcessStateChangedEvent) {
        log.info { "Handling expiry for process ${event.processId} of type ${event.processType}" }
        
        // TODO: Implement expiry-specific logic
        // - Clean up resources
        // - Cancel external requests
        // - Update related processes
    }
}