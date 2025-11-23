package ai.sovereignrag.process.action.lien

import ai.sovereignrag.commons.json.ObjectMapperFacade
import ai.sovereignrag.commons.process.ProcessGateway
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class UnlienAmountEventListener(private val processGateway: ProcessGateway) {

    private val log = logger {}

    @ApplicationModuleListener
    fun on(event: UnlienAmountEvent) {

        log.info { "Received Unlien Amount Event: ${ObjectMapperFacade.writeValueAsString(event)}" }

        processGateway.updateProcessRequestState(event.processPublicId, event.processRequestId, event.state)
    }
}