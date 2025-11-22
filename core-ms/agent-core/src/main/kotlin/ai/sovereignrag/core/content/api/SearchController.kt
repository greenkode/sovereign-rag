package ai.sovereignrag.core.content.api

import an.awesome.pipelinr.Pipeline
import mu.KotlinLogging
import nl.compilot.ai.content.dto.AutocompleteResponse
import nl.compilot.ai.content.dto.SearchRequest
import nl.compilot.ai.content.dto.SearchResponse
import nl.compilot.ai.content.query.AutocompleteQuery
import nl.compilot.ai.content.query.SearchDocumentsQuery
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class SearchController(
    private val pipeline: Pipeline
) {

    @PostMapping("/search")
    fun search(@RequestBody request: SearchRequest): SearchResponse {
        logger.info { "Search request: ${request.query}" }

        val query = SearchDocumentsQuery(
            query = request.query,
            numResults = request.numResults,
            minConfidence = request.minConfidence,
            lowConfidenceThreshold = request.lowConfidenceThreshold,
            returnMode = request.returnMode
        )

        return pipeline.send(query)
    }

    @GetMapping("/autocomplete")
    fun autocomplete(
        @RequestParam("q") query: String,
        @RequestParam(value = "limit", defaultValue = "5") limit: Int
    ): AutocompleteResponse {
        logger.info { "Autocomplete request: $query" }

        val autocompleteQuery = AutocompleteQuery(
            query = query,
            limit = limit
        )

        return pipeline.send(autocompleteQuery)
    }
}
