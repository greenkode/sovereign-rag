package ai.sovereignrag.commons.chunking

interface ChunkQualityMetric {
    val name: String
    val description: String

    fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult
}

data class MetricResult(
    val metricName: String,
    val score: Double,
    val details: Map<String, Any> = emptyMap(),
    val recommendations: List<String> = emptyList()
)

data class QualityReport(
    val overallScore: Double,
    val metricResults: Map<String, MetricResult>,
    val recommendations: List<String>,
    val chunkCount: Int,
    val averageChunkSize: Double,
    val processingTimeMs: Long
)

interface ChunkQualityEvaluator {
    fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>? = null): QualityReport
    fun addMetric(metric: ChunkQualityMetric)
    fun removeMetric(metricName: String)
}

data class QualityThresholds(
    val minimumOverallScore: Double = 0.7,
    val minimumCoherenceScore: Double = 0.6,
    val minimumBoundaryScore: Double = 0.5,
    val targetChunkSize: Int = 1000,
    val chunkSizeTolerance: Double = 0.3
)
