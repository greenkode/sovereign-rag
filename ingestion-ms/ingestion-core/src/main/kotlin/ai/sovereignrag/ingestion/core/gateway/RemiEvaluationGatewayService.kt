package ai.sovereignrag.ingestion.core.gateway

import ai.sovereignrag.commons.remi.RemiEvaluationGateway
import ai.sovereignrag.commons.remi.RemiEvaluationRequest
import ai.sovereignrag.commons.remi.RemiEvaluationResult
import ai.sovereignrag.commons.remi.RemiEvaluationStatus
import ai.sovereignrag.ingestion.commons.entity.EvaluationStatus
import ai.sovereignrag.ingestion.commons.entity.RemiMetrics
import ai.sovereignrag.ingestion.core.remi.RemiEvaluationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture
import ai.sovereignrag.ingestion.core.remi.RemiEvaluationRequest as ServiceRequest

private val log = KotlinLogging.logger {}

@Component
class RemiEvaluationGatewayService(
    private val remiEvaluationService: RemiEvaluationService
) : RemiEvaluationGateway {

    override fun evaluateAsync(request: RemiEvaluationRequest): CompletableFuture<RemiEvaluationResult> {
        log.info { "Triggering async REMI evaluation for query ${request.queryId}" }

        val serviceRequest = mapToServiceRequest(request)
        return remiEvaluationService.evaluateAsync(serviceRequest)
            .thenApply { mapToResult(it) }
    }

    override fun evaluate(request: RemiEvaluationRequest): RemiEvaluationResult {
        log.info { "Triggering REMI evaluation for query ${request.queryId}" }

        val serviceRequest = mapToServiceRequest(request)
        val metrics = remiEvaluationService.evaluate(serviceRequest)
        return mapToResult(metrics)
    }

    override fun findByQueryId(queryId: UUID): RemiEvaluationResult? {
        return remiEvaluationService.findByQueryId(queryId)?.let { mapToResult(it) }
    }

    private fun mapToServiceRequest(request: RemiEvaluationRequest): ServiceRequest {
        return ServiceRequest(
            organizationId = request.organizationId,
            knowledgeBaseId = request.knowledgeBaseId,
            queryId = request.queryId,
            queryText = request.queryText,
            generatedAnswer = request.generatedAnswer,
            retrievedChunks = request.retrievedChunks,
            retrievalMetricsId = request.retrievalMetricsId
        )
    }

    private fun mapToResult(metrics: RemiMetrics): RemiEvaluationResult {
        return RemiEvaluationResult(
            id = metrics.id ?: UUID.randomUUID(),
            queryId = metrics.queryId,
            answerRelevanceScore = metrics.answerRelevanceScore,
            answerRelevanceReasoning = metrics.answerRelevanceReasoning,
            contextRelevanceScore = metrics.contextRelevanceScore,
            contextRelevanceReasoning = metrics.contextRelevanceReasoning,
            groundednessScore = metrics.groundednessScore,
            groundednessReasoning = metrics.groundednessReasoning,
            overallScore = metrics.overallScore,
            hallucinationDetected = metrics.hallucinationDetected,
            missingKnowledgeDetected = metrics.missingKnowledgeDetected,
            status = mapStatus(metrics.evaluationStatus),
            evaluatedAt = metrics.evaluatedAt,
            evaluationTimeMs = metrics.evaluationTimeMs
        )
    }

    private fun mapStatus(status: EvaluationStatus): RemiEvaluationStatus {
        return when (status) {
            EvaluationStatus.PENDING -> RemiEvaluationStatus.PENDING
            EvaluationStatus.IN_PROGRESS -> RemiEvaluationStatus.IN_PROGRESS
            EvaluationStatus.COMPLETED -> RemiEvaluationStatus.COMPLETED
            EvaluationStatus.FAILED -> RemiEvaluationStatus.FAILED
        }
    }
}
