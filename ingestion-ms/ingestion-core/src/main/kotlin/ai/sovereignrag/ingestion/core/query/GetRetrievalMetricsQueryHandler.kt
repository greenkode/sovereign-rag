package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.entity.RetrievalMetrics
import ai.sovereignrag.ingestion.commons.repository.RetrievalMetricsRepository
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
class GetRetrievalMetricsQueryHandler(
    private val retrievalMetricsRepository: RetrievalMetricsRepository
) : Command.Handler<GetRetrievalMetricsQuery, RetrievalMetricsPagedResult> {

    override fun handle(query: GetRetrievalMetricsQuery): RetrievalMetricsPagedResult {
        log.info { "Processing GetRetrievalMetricsQuery for org ${query.organizationId}, kb ${query.knowledgeBaseId}" }

        val since = Instant.now().minus(query.days.toLong(), ChronoUnit.DAYS)
        val pageable = PageRequest.of(query.page, query.size)

        val metricsPage = retrievalMetricsRepository.findByOrganizationIdAndKnowledgeBaseId(
            query.organizationId, query.knowledgeBaseId, since, pageable
        )

        return RetrievalMetricsPagedResult(
            metrics = metricsPage.content.map { it.toResult() },
            page = query.page,
            size = query.size,
            totalElements = metricsPage.totalElements,
            totalPages = metricsPage.totalPages
        )
    }
}

fun RetrievalMetrics.toResult() = RetrievalMetricResult(
    id = id!!,
    queryId = queryId,
    queryEmbeddingTimeMs = queryEmbeddingTimeMs,
    searchTimeMs = searchTimeMs,
    totalTimeMs = totalTimeMs,
    resultsReturned = resultsReturned,
    topResultScore = topResultScore,
    averageResultScore = averageResultScore,
    distinctSourcesCount = distinctSourcesCount,
    userFeedbackScore = userFeedbackScore,
    clickedResultIndex = clickedResultIndex,
    embeddingModel = embeddingModel,
    queriedAt = queriedAt
)
