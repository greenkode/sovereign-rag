package ai.sovereignrag.ingestion.core.query

import an.awesome.pipelinr.Command
import java.time.Instant
import java.util.UUID

data class GetRetrievalMetricsQuery(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val days: Int = 7,
    val page: Int = 0,
    val size: Int = 20
) : Command<RetrievalMetricsPagedResult>

data class RetrievalMetricsPagedResult(
    val metrics: List<RetrievalMetricResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class RetrievalMetricResult(
    val id: UUID,
    val queryId: UUID?,
    val queryEmbeddingTimeMs: Long,
    val searchTimeMs: Long,
    val totalTimeMs: Long,
    val resultsReturned: Int,
    val topResultScore: Double?,
    val averageResultScore: Double?,
    val distinctSourcesCount: Int,
    val userFeedbackScore: Double?,
    val clickedResultIndex: Int?,
    val embeddingModel: String?,
    val queriedAt: Instant
)
