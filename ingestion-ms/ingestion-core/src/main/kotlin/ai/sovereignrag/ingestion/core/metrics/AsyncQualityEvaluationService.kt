package ai.sovereignrag.ingestion.core.metrics

import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.QualityReport
import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.ingestion.commons.entity.MetricSource
import ai.sovereignrag.ingestion.commons.entity.MetricType
import ai.sovereignrag.ingestion.commons.entity.QualityMetrics
import ai.sovereignrag.ingestion.commons.repository.QualityMetricsRepository
import ai.sovereignrag.ingestion.core.chunking.quality.DefaultChunkQualityEvaluator
import ai.sovereignrag.ingestion.core.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger {}

data class QualityEvaluationRequest(
    val organizationId: UUID,
    val knowledgeBaseId: UUID?,
    val knowledgeSourceId: UUID?,
    val ingestionJobId: UUID?,
    val chunks: List<DocumentChunk>,
    val embeddings: List<FloatArray>?,
    val chunkingStrategy: String?,
    val embeddingModel: String?,
    val metricSource: MetricSource = MetricSource.CHUNKING
)

data class RetrievalQualityRequest(
    val organizationId: UUID,
    val knowledgeBaseId: UUID?,
    val queryId: UUID,
    val queryText: String,
    val queryEmbeddingTimeMs: Long,
    val searchTimeMs: Long,
    val results: List<RetrievalResult>,
    val embeddingModel: String?,
    val searchStrategy: String?
)

data class RetrievalResult(
    val score: Double,
    val sourceId: UUID,
    val content: String
)

@Service
class AsyncQualityEvaluationService(
    private val qualityEvaluator: DefaultChunkQualityEvaluator,
    private val qualityMetricsRepository: QualityMetricsRepository,
    private val ingestionMetrics: IngestionMetrics,
    private val embeddingService: EmbeddingService
) {

    @Async("qualityEvaluationExecutor")
    fun evaluateChunkQualityAsync(request: QualityEvaluationRequest): CompletableFuture<QualityMetrics?> {
        return CompletableFuture.supplyAsync {
            evaluateChunkQuality(request)
        }
    }

    fun evaluateChunkQuality(request: QualityEvaluationRequest): QualityMetrics? {
        if (request.chunks.isEmpty()) {
            log.debug { "No chunks to evaluate for job ${request.ingestionJobId}" }
            return null
        }

        val startTime = System.currentTimeMillis()

        return runCatching {
            val report = qualityEvaluator.evaluate(request.chunks, request.embeddings)

            val processingTimeMs = System.currentTimeMillis() - startTime

            val metrics = createQualityMetrics(request, report, processingTimeMs)
            val savedMetrics = qualityMetricsRepository.save(metrics)

            recordPrometheusMetrics(request, report)

            ingestionMetrics.qualityEvaluationTimer.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)

            log.info {
                "Quality evaluation completed for job ${request.ingestionJobId}: " +
                "score=${report.overallScore}, chunks=${request.chunks.size}, time=${processingTimeMs}ms"
            }

            savedMetrics
        }.onFailure { e ->
            log.error(e) { "Failed to evaluate quality for job ${request.ingestionJobId}" }
        }.getOrNull()
    }

    @Async("qualityEvaluationExecutor")
    fun evaluateWithEmbeddingsAsync(
        request: QualityEvaluationRequest,
        modelConfig: EmbeddingModelConfig
    ): CompletableFuture<QualityMetrics?> {
        return CompletableFuture.supplyAsync {
            evaluateWithEmbeddings(request, modelConfig)
        }
    }

    fun evaluateWithEmbeddings(
        request: QualityEvaluationRequest,
        modelConfig: EmbeddingModelConfig
    ): QualityMetrics? {
        if (request.chunks.isEmpty()) return null

        val embeddings = request.embeddings ?: runCatching {
            embeddingService.generateEmbeddings(
                request.chunks.map { it.content },
                modelConfig
            )
        }.getOrNull()

        return evaluateChunkQuality(request.copy(embeddings = embeddings))
    }

    private fun createQualityMetrics(
        request: QualityEvaluationRequest,
        report: QualityReport,
        processingTimeMs: Long
    ): QualityMetrics {
        val chunkSizes = request.chunks.map { it.content.length }

        return QualityMetrics(
            organizationId = request.organizationId,
            knowledgeBaseId = request.knowledgeBaseId,
            metricType = MetricType.WRITE_TIME,
            metricSource = request.metricSource
        ).apply {
            this.knowledgeSourceId = request.knowledgeSourceId
            this.ingestionJobId = request.ingestionJobId
            this.overallScore = report.overallScore
            this.coherenceScore = report.metricResults["internal_coherence"]?.score
            this.boundaryScore = report.metricResults["boundary_quality"]?.score
            this.sizeDistributionScore = report.metricResults["size_distribution"]?.score
            this.contextSufficiencyScore = report.metricResults["context_sufficiency"]?.score
            this.informationPreservationScore = report.metricResults["information_preservation"]?.score
            this.chunkCount = report.chunkCount
            this.averageChunkSize = report.averageChunkSize
            this.minChunkSize = chunkSizes.minOrNull()
            this.maxChunkSize = chunkSizes.maxOrNull()
            this.chunkingStrategy = request.chunkingStrategy
            this.embeddingModel = request.embeddingModel
            this.processingTimeMs = processingTimeMs
            this.evaluatedAt = Instant.now()
        }
    }

    private fun recordPrometheusMetrics(request: QualityEvaluationRequest, report: QualityReport) {
        request.knowledgeBaseId?.let { kbId ->
            ingestionMetrics.recordQualityMetrics(
                knowledgeBaseId = kbId.toString(),
                overallScore = report.overallScore,
                coherenceScore = report.metricResults["internal_coherence"]?.score,
                boundaryScore = report.metricResults["boundary_quality"]?.score,
                chunkCount = report.chunkCount,
                averageChunkSize = report.averageChunkSize
            )
        }
    }
}
