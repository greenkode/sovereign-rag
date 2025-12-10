package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.entity.RemiMetrics
import an.awesome.pipelinr.Command
import java.time.Instant
import java.util.UUID

data class GetRemiMetricsQuery(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val days: Int = 7,
    val filter: RemiFilter? = null,
    val page: Int = 0,
    val size: Int = 20
) : Command<RemiMetricsPagedResult>

enum class RemiFilter {
    ALL,
    HALLUCINATIONS,
    MISSING_KNOWLEDGE
}

data class RemiMetricsPagedResult(
    val metrics: List<RemiMetricResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class RemiMetricResult(
    val id: UUID,
    val queryId: UUID,
    val queryText: String,
    val generatedAnswer: String?,
    val answerRelevanceScore: Double?,
    val answerRelevanceReasoning: String?,
    val contextRelevanceScore: Double?,
    val contextRelevanceReasoning: String?,
    val groundednessScore: Double?,
    val groundednessReasoning: String?,
    val overallScore: Double?,
    val hallucinationDetected: Boolean,
    val missingKnowledgeDetected: Boolean,
    val retrievedChunksCount: Int,
    val evaluationTimeMs: Long,
    val evaluationStatus: String,
    val evaluatedAt: Instant?
)

fun RemiMetrics.toResult() = RemiMetricResult(
    id = id!!,
    queryId = queryId,
    queryText = queryText,
    generatedAnswer = generatedAnswer,
    answerRelevanceScore = answerRelevanceScore,
    answerRelevanceReasoning = answerRelevanceReasoning,
    contextRelevanceScore = contextRelevanceScore,
    contextRelevanceReasoning = contextRelevanceReasoning,
    groundednessScore = groundednessScore,
    groundednessReasoning = groundednessReasoning,
    overallScore = overallScore,
    hallucinationDetected = hallucinationDetected,
    missingKnowledgeDetected = missingKnowledgeDetected,
    retrievedChunksCount = retrievedChunksCount,
    evaluationTimeMs = evaluationTimeMs,
    evaluationStatus = evaluationStatus.name,
    evaluatedAt = evaluatedAt
)
