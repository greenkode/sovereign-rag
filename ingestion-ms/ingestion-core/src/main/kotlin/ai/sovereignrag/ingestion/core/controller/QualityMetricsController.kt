package ai.sovereignrag.ingestion.core.controller

import ai.sovereignrag.ingestion.commons.entity.MetricSource
import ai.sovereignrag.ingestion.core.command.RecordUserFeedbackCommand
import ai.sovereignrag.ingestion.core.query.GetQualityMetricsQuery
import ai.sovereignrag.ingestion.core.query.GetRetrievalMetricsQuery
import ai.sovereignrag.ingestion.core.query.QualityMetricsPagedResult
import ai.sovereignrag.ingestion.core.query.RetrievalMetricResult
import ai.sovereignrag.ingestion.core.query.RetrievalMetricsPagedResult
import ai.sovereignrag.ingestion.core.service.SecurityContextService
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/metrics")
@Tag(name = "Quality Metrics", description = "Ingestion and retrieval quality metrics endpoints")
@SecurityRequirement(name = "bearerAuth")
class QualityMetricsController(
    private val pipeline: Pipeline,
    private val securityContextService: SecurityContextService
) {

    @GetMapping("/quality")
    @Operation(summary = "Get quality metrics", description = "Get paginated write-time quality metrics for a knowledge base")
    fun getQualityMetrics(
        @PathVariable knowledgeBaseId: UUID,
        @RequestParam(defaultValue = "7") days: Int,
        @RequestParam(required = false) source: MetricSource?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): QualityMetricsPagedResult {
        val organizationId = getCurrentOrganizationId()
        log.info { "Getting quality metrics for knowledge base $knowledgeBaseId, org $organizationId, page $page" }
        return pipeline.send(
            GetQualityMetricsQuery(
                organizationId = organizationId,
                knowledgeBaseId = knowledgeBaseId,
                days = days,
                source = source,
                page = page,
                size = size
            )
        )
    }

    @GetMapping("/retrieval")
    @Operation(summary = "Get retrieval metrics", description = "Get paginated read-time retrieval metrics for a knowledge base")
    fun getRetrievalMetrics(
        @PathVariable knowledgeBaseId: UUID,
        @RequestParam(defaultValue = "7") days: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RetrievalMetricsPagedResult {
        val organizationId = getCurrentOrganizationId()
        log.info { "Getting retrieval metrics for knowledge base $knowledgeBaseId, org $organizationId, page $page" }
        return pipeline.send(
            GetRetrievalMetricsQuery(
                organizationId = organizationId,
                knowledgeBaseId = knowledgeBaseId,
                days = days,
                page = page,
                size = size
            )
        )
    }

    @PostMapping("/retrieval/{queryId}/feedback")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Record user feedback", description = "Record user feedback for a retrieval query")
    fun recordUserFeedback(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable queryId: UUID,
        @RequestBody request: UserFeedbackRequest
    ): RetrievalMetricResult? {
        val organizationId = getCurrentOrganizationId()
        log.info { "Recording feedback for query $queryId in knowledge base $knowledgeBaseId" }
        return pipeline.send(
            RecordUserFeedbackCommand(
                organizationId = organizationId,
                knowledgeBaseId = knowledgeBaseId,
                queryId = queryId,
                score = request.score,
                clickedIndex = request.clickedIndex,
                relevanceRatings = request.relevanceRatings
            )
        )
    }

    private fun getCurrentOrganizationId(): UUID = securityContextService.getCurrentMerchantId()
}

data class UserFeedbackRequest(
    val score: Double,
    val clickedIndex: Int?,
    val relevanceRatings: Map<Int, Int>?
)
