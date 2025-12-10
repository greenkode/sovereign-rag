package ai.sovereignrag.ingestion.core.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class IngestionMetrics(private val meterRegistry: MeterRegistry) {

    private val qualityScores = ConcurrentHashMap<String, AtomicReference<Double>>()
    private val chunkCounts = ConcurrentHashMap<String, AtomicReference<Double>>()

    val chunkingTimer: Timer = Timer.builder("ingestion.chunking.duration")
        .description("Time spent chunking documents")
        .register(meterRegistry)

    val embeddingTimer: Timer = Timer.builder("ingestion.embedding.duration")
        .description("Time spent generating embeddings")
        .register(meterRegistry)

    val qualityEvaluationTimer: Timer = Timer.builder("ingestion.quality.evaluation.duration")
        .description("Time spent evaluating chunk quality")
        .register(meterRegistry)

    val chunksCreatedCounter: Counter = Counter.builder("ingestion.chunks.created")
        .description("Total number of chunks created")
        .register(meterRegistry)

    val embeddingsCreatedCounter: Counter = Counter.builder("ingestion.embeddings.created")
        .description("Total number of embeddings created")
        .register(meterRegistry)

    val qualityEvaluationsCounter: Counter = Counter.builder("ingestion.quality.evaluations")
        .description("Total number of quality evaluations performed")
        .register(meterRegistry)

    val lowQualityChunksCounter: Counter = Counter.builder("ingestion.chunks.low_quality")
        .description("Number of chunks with quality score below threshold")
        .register(meterRegistry)

    val chunkSizeDistribution: DistributionSummary = DistributionSummary.builder("ingestion.chunk.size")
        .description("Distribution of chunk sizes")
        .baseUnit("characters")
        .register(meterRegistry)

    val qualityScoreDistribution: DistributionSummary = DistributionSummary.builder("ingestion.quality.score")
        .description("Distribution of quality scores")
        .register(meterRegistry)

    val coherenceScoreDistribution: DistributionSummary = DistributionSummary.builder("ingestion.quality.coherence")
        .description("Distribution of coherence scores")
        .register(meterRegistry)

    val boundaryScoreDistribution: DistributionSummary = DistributionSummary.builder("ingestion.quality.boundary")
        .description("Distribution of boundary scores")
        .register(meterRegistry)

    fun recordChunkingMetrics(
        knowledgeBaseId: String,
        chunkCount: Int,
        processingTimeMs: Long,
        strategy: String
    ) {
        chunkingTimer.record(java.time.Duration.ofMillis(processingTimeMs))
        chunksCreatedCounter.increment(chunkCount.toDouble())

        Counter.builder("ingestion.chunking.by_strategy")
            .tag("strategy", strategy)
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .increment()
    }

    fun recordEmbeddingMetrics(
        knowledgeBaseId: String,
        embeddingCount: Int,
        processingTimeMs: Long,
        model: String
    ) {
        embeddingTimer.record(java.time.Duration.ofMillis(processingTimeMs))
        embeddingsCreatedCounter.increment(embeddingCount.toDouble())

        Counter.builder("ingestion.embedding.by_model")
            .tag("model", model)
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .increment()
    }

    fun recordQualityMetrics(
        knowledgeBaseId: String,
        overallScore: Double,
        coherenceScore: Double?,
        boundaryScore: Double?,
        chunkCount: Int,
        averageChunkSize: Double
    ) {
        qualityEvaluationsCounter.increment()
        qualityScoreDistribution.record(overallScore)

        coherenceScore?.let { coherenceScoreDistribution.record(it) }
        boundaryScore?.let { boundaryScoreDistribution.record(it) }

        repeat(chunkCount) {
            chunkSizeDistribution.record(averageChunkSize)
        }

        if (overallScore < 0.6) {
            lowQualityChunksCounter.increment()
        }

        updateKnowledgeBaseGauge(knowledgeBaseId, overallScore, chunkCount)
    }

    fun recordRetrievalMetrics(
        knowledgeBaseId: String,
        searchTimeMs: Long,
        resultsReturned: Int,
        topScore: Double?,
        averageScore: Double?
    ) {
        Timer.builder("ingestion.retrieval.search.duration")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .record(java.time.Duration.ofMillis(searchTimeMs))

        Counter.builder("ingestion.retrieval.queries")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .increment()

        DistributionSummary.builder("ingestion.retrieval.results_count")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .record(resultsReturned.toDouble())

        topScore?.let {
            DistributionSummary.builder("ingestion.retrieval.top_score")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it)
        }

        averageScore?.let {
            DistributionSummary.builder("ingestion.retrieval.average_score")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it)
        }
    }

    fun recordUserFeedback(
        knowledgeBaseId: String,
        queryId: String,
        feedbackScore: Double,
        clickedIndex: Int?
    ) {
        DistributionSummary.builder("ingestion.retrieval.user_feedback")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .record(feedbackScore)

        clickedIndex?.let {
            DistributionSummary.builder("ingestion.retrieval.clicked_position")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it.toDouble())
        }
    }

    fun recordRemiEvaluation(
        knowledgeBaseId: String,
        answerRelevance: Double?,
        contextRelevance: Double?,
        groundedness: Double?,
        evaluationTimeMs: Long
    ) {
        Counter.builder("ingestion.remi.evaluations")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .increment()

        Timer.builder("ingestion.remi.evaluation.duration")
            .tag("knowledge_base_id", knowledgeBaseId)
            .register(meterRegistry)
            .record(java.time.Duration.ofMillis(evaluationTimeMs))

        answerRelevance?.let {
            DistributionSummary.builder("ingestion.remi.answer_relevance")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it)
        }

        contextRelevance?.let {
            DistributionSummary.builder("ingestion.remi.context_relevance")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it)

            if (it < 0.3) {
                Counter.builder("ingestion.remi.missing_knowledge")
                    .tag("knowledge_base_id", knowledgeBaseId)
                    .register(meterRegistry)
                    .increment()
            }
        }

        groundedness?.let {
            DistributionSummary.builder("ingestion.remi.groundedness")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(it)

            if (it < 0.5) {
                Counter.builder("ingestion.remi.hallucinations")
                    .tag("knowledge_base_id", knowledgeBaseId)
                    .register(meterRegistry)
                    .increment()
            }
        }

        val scores = listOfNotNull(answerRelevance, contextRelevance, groundedness)
        scores.takeIf { it.isNotEmpty() }?.average()?.let { overall ->
            DistributionSummary.builder("ingestion.remi.overall_score")
                .tag("knowledge_base_id", knowledgeBaseId)
                .register(meterRegistry)
                .record(overall)
        }
    }

    private fun updateKnowledgeBaseGauge(knowledgeBaseId: String, score: Double, chunkCount: Int) {
        val scoreRef = qualityScores.computeIfAbsent(knowledgeBaseId) { id ->
            val ref = AtomicReference(0.0)
            Gauge.builder("ingestion.quality.current_score") { ref.get() }
                .tag("knowledge_base_id", id)
                .description("Current quality score for knowledge base")
                .register(meterRegistry)
            ref
        }
        scoreRef.set(score)

        val countRef = chunkCounts.computeIfAbsent(knowledgeBaseId) { id ->
            val ref = AtomicReference(0.0)
            Gauge.builder("ingestion.chunks.total") { ref.get() }
                .tag("knowledge_base_id", id)
                .description("Total chunks in knowledge base")
                .register(meterRegistry)
            ref
        }
        countRef.updateAndGet { it + chunkCount }
    }
}
