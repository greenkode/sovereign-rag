package ai.sovereignrag.commons.remi

import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface RemiEvaluationGateway {

    fun evaluateAsync(request: RemiEvaluationRequest): CompletableFuture<RemiEvaluationResult>

    fun evaluate(request: RemiEvaluationRequest): RemiEvaluationResult

    fun findByQueryId(queryId: UUID): RemiEvaluationResult?
}

data class RemiEvaluationRequest(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val queryId: UUID,
    val queryText: String,
    val generatedAnswer: String,
    val retrievedChunks: List<String>,
    val retrievalMetricsId: UUID? = null,
    val modelUsed: String? = null
)

data class RemiEvaluationResult(
    val id: UUID,
    val queryId: UUID,
    val answerRelevanceScore: Double?,
    val answerRelevanceReasoning: String?,
    val contextRelevanceScore: Double?,
    val contextRelevanceReasoning: String?,
    val groundednessScore: Double?,
    val groundednessReasoning: String?,
    val overallScore: Double?,
    val hallucinationDetected: Boolean,
    val missingKnowledgeDetected: Boolean,
    val status: RemiEvaluationStatus,
    val evaluatedAt: Instant?,
    val evaluationTimeMs: Long?
)

enum class RemiEvaluationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
