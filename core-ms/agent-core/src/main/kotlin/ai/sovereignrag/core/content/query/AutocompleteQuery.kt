package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import nl.compilot.ai.content.dto.AutocompleteResponse

data class AutocompleteQuery(
    val query: String,
    val limit: Int = 5
) : Command<AutocompleteResponse>
