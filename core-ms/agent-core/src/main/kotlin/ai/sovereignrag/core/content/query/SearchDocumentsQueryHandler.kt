package ai.sovereignrag.core.content.query

import an.awesome.pipelinr.Command
import mu.KotlinLogging
import ai.sovereignrag.config.SovereignRagProperties
import ai.sovereignrag.content.dto.SearchResponse
import ai.sovereignrag.content.service.ContentService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SearchDocumentsQueryHandler(
    private val contentService: ContentService,
    private val properties: SovereignRagProperties
) : Command.Handler<SearchDocumentsQuery, SearchResponse> {

    override fun handle(query: SearchDocumentsQuery): SearchResponse {
        logger.info { "Handling SearchDocumentsQuery: ${query.query}" }

        val results = contentService.search(
            query = query.query,
            numResults = query.numResults.coerceAtMost(properties.knowledgeGraph.maxResults),
            minConfidence = query.minConfidence
        )

        val threshold = query.lowConfidenceThreshold
            ?: properties.knowledgeGraph.lowConfidenceThreshold
        val maxConfidence = results.maxOfOrNull { it.confidence } ?: 0.0
        val flagged = results.isNotEmpty() && maxConfidence < threshold

        val filteredResults = if (query.returnMode == "single") {
            results.take(1)
        } else {
            results
        }

        logger.info { "Search completed: ${filteredResults.size} results, flagged: $flagged" }

        return SearchResponse(
            results = filteredResults,
            flagged = flagged,
            flagReason = if (flagged) {
                "Low confidence results (max: ${String.format("%.2f", maxConfidence)})"
            } else null,
            threshold = if (flagged) threshold else null
        )
    }
}
