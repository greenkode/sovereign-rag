package ai.sovereignrag.core.content.event

import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service to publish content-related events
 */
@Service
class ContentEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishIngestionEvent(event: ContentIngestionEvent) {
        logger.debug { "[${event.tenantId}] Publishing content ingestion event for: ${event.title}" }
        applicationEventPublisher.publishEvent(event)
    }
}
