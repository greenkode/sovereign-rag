package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.entity.QualityMetrics
import ai.sovereignrag.ingestion.commons.repository.QualityMetricsRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetQualityMetricsQueryHandler(
    private val qualityMetricsRepository: QualityMetricsRepository
) : Command.Handler<GetQualityMetricsQuery, QualityMetricsPagedResult> {

    override fun handle(query: GetQualityMetricsQuery): QualityMetricsPagedResult {
        log.info { "Processing GetQualityMetricsQuery for org ${query.organizationId}, kb ${query.knowledgeBaseId}" }

        val since = Instant.now().minus(query.days.toLong(), ChronoUnit.DAYS)
        val pageable = PageRequest.of(query.page, query.size)

        val metricsPage = query.source?.let {
            qualityMetricsRepository.findByOrganizationIdAndKnowledgeBaseIdAndMetricSource(
                query.organizationId, query.knowledgeBaseId, it, since, pageable
            )
        } ?: qualityMetricsRepository.findByOrganizationIdAndKnowledgeBaseId(
            query.organizationId, query.knowledgeBaseId, since, pageable
        )

        return QualityMetricsPagedResult(
            metrics = metricsPage.content.map { it.toResult() },
            page = query.page,
            size = query.size,
            totalElements = metricsPage.totalElements,
            totalPages = metricsPage.totalPages
        )
    }
}

fun QualityMetrics.toResult() = QualityMetricResult(
    id = id!!,
    knowledgeSourceId = knowledgeSourceId,
    ingestionJobId = ingestionJobId,
    overallScore = overallScore,
    coherenceScore = coherenceScore,
    boundaryScore = boundaryScore,
    sizeDistributionScore = sizeDistributionScore,
    contextSufficiencyScore = contextSufficiencyScore,
    chunkCount = chunkCount,
    averageChunkSize = averageChunkSize,
    chunkingStrategy = chunkingStrategy,
    embeddingModel = embeddingModel,
    processingTimeMs = processingTimeMs,
    evaluatedAt = evaluatedAt
)
