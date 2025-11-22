package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import nl.compilot.ai.content.dto.AutocompleteResponse
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class AutocompleteQueryHandler : Command.Handler<AutocompleteQuery, AutocompleteResponse> {

    override fun handle(query: AutocompleteQuery): AutocompleteResponse {
        logger.debug { "Handling AutocompleteQuery: ${query.query}" }

        // Simplified: autocomplete not supported in current implementation
        // Can be added later by indexing titles/entities separately
        return AutocompleteResponse(suggestions = emptyList())
    }
}
