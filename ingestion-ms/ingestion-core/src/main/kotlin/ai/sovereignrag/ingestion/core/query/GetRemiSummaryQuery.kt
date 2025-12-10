package ai.sovereignrag.ingestion.core.query

import an.awesome.pipelinr.Command
import java.util.UUID

data class GetRemiSummaryQuery(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val days: Int = 7
) : Command<RemiSummaryResult>

data class RemiSummaryResult(
    val averageOverallScore: Double?,
    val averageAnswerRelevance: Double?,
    val averageContextRelevance: Double?,
    val averageGroundedness: Double?,
    val hallucinationCount: Long,
    val missingKnowledgeCount: Long,
    val totalEvaluations: Long,
    val healthStatus: RemiHealthStatus
)

enum class RemiHealthStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
    NO_DATA
}
