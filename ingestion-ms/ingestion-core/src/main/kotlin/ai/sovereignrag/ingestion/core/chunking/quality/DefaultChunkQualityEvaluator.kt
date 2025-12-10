package ai.sovereignrag.ingestion.core.chunking.quality

import ai.sovereignrag.commons.chunking.ChunkQualityEvaluator
import ai.sovereignrag.commons.chunking.ChunkQualityMetric
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.QualityReport
import ai.sovereignrag.commons.chunking.QualityThresholds
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

private val log = KotlinLogging.logger {}

@Component
class DefaultChunkQualityEvaluator(
    private val internalCoherenceMetric: InternalCoherenceMetric,
    private val boundaryQualityMetric: BoundaryQualityMetric,
    private val sizeDistributionMetric: SizeDistributionMetric,
    private val informationPreservationMetric: InformationPreservationMetric,
    private val contextSufficiencyMetric: ContextSufficiencyMetric
) : ChunkQualityEvaluator {

    private val metrics = mutableMapOf<String, ChunkQualityMetric>()
    private val weights = mutableMapOf<String, Double>()
    private var thresholds = QualityThresholds()

    init {
        addMetric(sizeDistributionMetric, 0.25)
        addMetric(informationPreservationMetric, 0.25)
        addMetric(contextSufficiencyMetric, 0.25)
        addMetric(boundaryQualityMetric, 0.15)
        addMetric(internalCoherenceMetric, 0.10)
    }

    fun addMetric(metric: ChunkQualityMetric, weight: Double) {
        metrics[metric.name] = metric
        weights[metric.name] = weight
    }

    override fun addMetric(metric: ChunkQualityMetric) {
        addMetric(metric, 1.0)
    }

    override fun removeMetric(metricName: String) {
        metrics.remove(metricName)
        weights.remove(metricName)
    }

    fun setThresholds(thresholds: QualityThresholds) {
        this.thresholds = thresholds
    }

    fun setWeight(metricName: String, weight: Double) {
        if (metrics.containsKey(metricName)) {
            weights[metricName] = weight
        }
    }

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): QualityReport {
        log.debug { "Evaluating quality for ${chunks.size} chunks" }

        var processingTimeMs: Long
        val metricResults = mutableMapOf<String, ai.sovereignrag.commons.chunking.MetricResult>()

        processingTimeMs = measureTimeMillis {
            for ((name, metric) in metrics) {
                runCatching {
                    metricResults[name] = metric.evaluate(chunks, embeddings)
                }.onFailure { e ->
                    log.warn(e) { "Failed to evaluate metric: $name" }
                    metricResults[name] = ai.sovereignrag.commons.chunking.MetricResult(
                        metricName = name,
                        score = 0.0,
                        details = mapOf("error" to (e.message ?: "Unknown error"))
                    )
                }
            }
        }

        val totalWeight = weights.values.sum()
        val overallScore = metricResults.entries.sumOf { (name, result) ->
            result.score * (weights[name] ?: 0.0)
        } / totalWeight

        val recommendations = generateRecommendations(metricResults, overallScore)
        val averageChunkSize = chunks.map { it.content.length }.average()

        return QualityReport(
            overallScore = overallScore,
            metricResults = metricResults,
            recommendations = recommendations,
            chunkCount = chunks.size,
            averageChunkSize = averageChunkSize,
            processingTimeMs = processingTimeMs
        )
    }

    private fun generateRecommendations(
        results: Map<String, ai.sovereignrag.commons.chunking.MetricResult>,
        overallScore: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        results.values.flatMap { it.recommendations }.forEach { rec ->
            if (rec !in recommendations) {
                recommendations.add(rec)
            }
        }

        if (overallScore < thresholds.minimumOverallScore) {
            recommendations.add(
                0,
                "Overall quality score (${String.format("%.2f", overallScore)}) is below threshold (${thresholds.minimumOverallScore})"
            )
        }

        return recommendations
    }

    fun evaluateWithDetailedReport(
        chunks: List<DocumentChunk>,
        embeddings: List<FloatArray>?,
        sentenceEmbeddings: List<List<FloatArray>>? = null
    ): DetailedQualityReport {
        val baseReport = evaluate(chunks, embeddings)

        val coherenceWithSentences = sentenceEmbeddings?.let {
            internalCoherenceMetric.evaluateWithSentenceEmbeddings(chunks, it)
        }

        val passesThresholds = baseReport.overallScore >= thresholds.minimumOverallScore

        return DetailedQualityReport(
            baseReport = baseReport,
            thresholds = thresholds,
            passesThresholds = passesThresholds,
            detailedCoherence = coherenceWithSentences,
            strategyDistribution = chunks.groupBy { it.metadata.strategyUsed ?: "unknown" }
                .mapValues { it.value.size }
        )
    }
}

data class DetailedQualityReport(
    val baseReport: QualityReport,
    val thresholds: QualityThresholds,
    val passesThresholds: Boolean,
    val detailedCoherence: ai.sovereignrag.commons.chunking.MetricResult?,
    val strategyDistribution: Map<String, Int>
)
