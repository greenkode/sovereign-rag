package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.entity.MetricSource
import an.awesome.pipelinr.Command
import java.time.Instant
import java.util.UUID

data class GetQualityMetricsQuery(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val days: Int = 7,
    val source: MetricSource? = null,
    val page: Int = 0,
    val size: Int = 20
) : Command<QualityMetricsPagedResult>

data class QualityMetricsPagedResult(
    val metrics: List<QualityMetricResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class QualityMetricResult(
    val id: UUID,
    val knowledgeSourceId: UUID?,
    val ingestionJobId: UUID?,
    val overallScore: Double,
    val coherenceScore: Double?,
    val boundaryScore: Double?,
    val sizeDistributionScore: Double?,
    val contextSufficiencyScore: Double?,
    val chunkCount: Int,
    val averageChunkSize: Double,
    val chunkingStrategy: String?,
    val embeddingModel: String?,
    val processingTimeMs: Long,
    val evaluatedAt: Instant
)
