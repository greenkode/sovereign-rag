package ai.sovereignrag.identity.process.event

import ai.sovereignrag.identity.process.domain.model.ProcessStateChangedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AsyncEventPublisher(
    private val eventPublisher: ApplicationEventPublisher
) {
    
    private val log = KotlinLogging.logger {}
    
    @Async
    fun publishStateChangeEvent(event: ProcessStateChangedEvent) {
        try {
            log.debug { "Publishing async state change event for process ${event.processId}" }
            eventPublisher.publishEvent(event)
        } catch (e: Exception) {
            log.error(e) { "Failed to publish state change event for process ${event.processId}" }
        }
    }
}