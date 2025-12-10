package ai.sovereignrag.ingestion.core.metrics

import ai.sovereignrag.ingestion.commons.entity.RetrievalMetrics
import ai.sovereignrag.ingestion.commons.repository.RetrievalMetricsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger {}

@Service
class RetrievalMetricsService(
    private val retrievalMetricsRepository: RetrievalMetricsRepository,
    private val ingestionMetrics: IngestionMetrics
) {

    @Async("qualityEvaluationExecutor")
    fun recordRetrievalAsync(request: RetrievalQualityRequest): CompletableFuture<RetrievalMetrics> {
        return CompletableFuture.supplyAsync {
            recordRetrieval(request)
        }
    }

    fun recordRetrieval(request: RetrievalQualityRequest): RetrievalMetrics {
        val scores = request.results.map { it.score }
        val topScore = scores.maxOrNull()
        val avgScore = scores.takeIf { it.isNotEmpty() }?.average()
        val lowestScore = scores.minOrNull()
        val variance = calculateVariance(scores)
        val distinctSources = request.results.map { it.sourceId }.distinct().size

        val metrics = RetrievalMetrics(
            organizationId = request.organizationId,
            knowledgeBaseId = request.knowledgeBaseId
        ).apply {
            this.queryId = request.queryId
            this.queryText = request.queryText.take(500)
            this.queryEmbeddingTimeMs = request.queryEmbeddingTimeMs
            this.searchTimeMs = request.searchTimeMs
            this.totalTimeMs = request.queryEmbeddingTimeMs + request.searchTimeMs
            this.resultsReturned = request.results.size
            this.topResultScore = topScore
            this.averageResultScore = avgScore
            this.lowestResultScore = lowestScore
            this.scoreVariance = variance
            this.distinctSourcesCount = distinctSources
            this.embeddingModel = request.embeddingModel
            this.searchStrategy = request.searchStrategy
            this.queriedAt = Instant.now()
        }

        val saved = retrievalMetricsRepository.save(metrics)

        request.knowledgeBaseId?.let { kbId ->
            ingestionMetrics.recordRetrievalMetrics(
                knowledgeBaseId = kbId.toString(),
                searchTimeMs = request.searchTimeMs,
                resultsReturned = request.results.size,
                topScore = topScore,
                averageScore = avgScore
            )
        }

        log.debug {
            "Recorded retrieval metrics for query ${request.queryId}: " +
            "results=${request.results.size}, topScore=$topScore, searchTime=${request.searchTimeMs}ms"
        }

        return saved
    }

    @Async("qualityEvaluationExecutor")
    fun recordUserFeedbackAsync(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        queryId: UUID,
        feedbackScore: Double,
        clickedResultIndex: Int?,
        relevanceRatings: Map<Int, Int>?
    ): CompletableFuture<RetrievalMetrics?> {
        return CompletableFuture.supplyAsync {
            recordUserFeedback(organizationId, knowledgeBaseId, queryId, feedbackScore, clickedResultIndex, relevanceRatings)
        }
    }

    fun recordUserFeedback(
        organizationId: UUID,
        knowledgeBaseId: UUID,
        queryId: UUID,
        feedbackScore: Double,
        clickedResultIndex: Int?,
        relevanceRatings: Map<Int, Int>?
    ): RetrievalMetrics? {
        val metrics = retrievalMetricsRepository.findByQueryId(queryId) ?: run {
            log.warn { "No retrieval metrics found for query $queryId" }
            return null
        }

        require(metrics.organizationId == organizationId) {
            "Metrics record does not belong to organization $organizationId"
        }
        require(metrics.knowledgeBaseId == knowledgeBaseId) {
            "Metrics record does not belong to knowledge base $knowledgeBaseId"
        }

        metrics.userFeedbackScore = feedbackScore
        metrics.clickedResultIndex = clickedResultIndex
        relevanceRatings?.let {
            metrics.resultRelevanceRatings = it.entries.joinToString(",") { (k, v) -> "$k:$v" }
        }

        val saved = retrievalMetricsRepository.save(metrics)

        ingestionMetrics.recordUserFeedback(
            knowledgeBaseId = knowledgeBaseId.toString(),
            queryId = queryId.toString(),
            feedbackScore = feedbackScore,
            clickedIndex = clickedResultIndex
        )

        log.info { "Recorded user feedback for query $queryId: score=$feedbackScore" }

        return saved
    }

    private fun calculateVariance(scores: List<Double>): Double? {
        if (scores.size < 2) return null
        val mean = scores.average()
        return scores.map { (it - mean) * (it - mean) }.average()
    }
}
