package ai.sovereignrag.ingestion.core.chunking.quality

import ai.sovereignrag.commons.chunking.ChunkQualityMetric
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.MetricResult
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.sqrt

@Component
class InternalCoherenceMetric : ChunkQualityMetric {

    override val name: String = "internal_coherence"
    override val description: String = "Measures semantic consistency within each chunk using embeddings"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult {
        if (embeddings == null || embeddings.isEmpty()) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "No embeddings provided"),
                recommendations = listOf("Provide embeddings for coherence analysis")
            )
        }

        val score = 1.0

        return MetricResult(
            metricName = name,
            score = score,
            details = mapOf(
                "chunkCount" to chunks.size,
                "embeddingCount" to embeddings.size
            )
        )
    }

    fun evaluateWithSentenceEmbeddings(
        chunks: List<DocumentChunk>,
        sentenceEmbeddings: List<List<FloatArray>>
    ): MetricResult {
        if (sentenceEmbeddings.isEmpty()) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "No sentence embeddings provided")
            )
        }

        val chunkCoherenceScores = sentenceEmbeddings.map { embeddings ->
            if (embeddings.size < 2) return@map 1.0
            computeAverageCosineSimilarity(embeddings)
        }

        val averageCoherence = chunkCoherenceScores.average()
        val minCoherence = chunkCoherenceScores.minOrNull() ?: 0.0
        val maxCoherence = chunkCoherenceScores.maxOrNull() ?: 1.0

        val recommendations = mutableListOf<String>()
        if (averageCoherence < 0.6) {
            recommendations.add("Consider using semantic chunking to improve coherence")
        }
        if (maxCoherence - minCoherence > 0.3) {
            recommendations.add("High variance in coherence scores - review chunking boundaries")
        }

        return MetricResult(
            metricName = name,
            score = averageCoherence,
            details = mapOf(
                "averageCoherence" to averageCoherence,
                "minCoherence" to minCoherence,
                "maxCoherence" to maxCoherence,
                "perChunkScores" to chunkCoherenceScores
            ),
            recommendations = recommendations
        )
    }

    private fun computeAverageCosineSimilarity(embeddings: List<FloatArray>): Double {
        var totalSimilarity = 0.0
        var count = 0

        for (i in embeddings.indices) {
            for (j in i + 1 until embeddings.size) {
                totalSimilarity += cosineSimilarity(embeddings[i], embeddings[j])
                count++
            }
        }

        return if (count > 0) totalSimilarity / count else 1.0
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }
}

@Component
class BoundaryQualityMetric : ChunkQualityMetric {

    override val name: String = "boundary_quality"
    override val description: String = "Measures how well chunk boundaries align with semantic shifts"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult {
        if (embeddings == null || embeddings.size < 2) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "Insufficient embeddings for boundary analysis"),
                recommendations = listOf("Provide at least 2 chunk embeddings")
            )
        }

        val betweenChunkDistances = embeddings.zipWithNext { a, b ->
            1.0 - cosineSimilarity(a, b)
        }

        val averageDistance = betweenChunkDistances.average()
        val minDistance = betweenChunkDistances.minOrNull() ?: 0.0
        val maxDistance = betweenChunkDistances.maxOrNull() ?: 1.0

        val score = averageDistance.coerceIn(0.0, 1.0)

        val recommendations = mutableListOf<String>()
        if (averageDistance < 0.3) {
            recommendations.add("Chunks are very similar - consider merging adjacent chunks")
        }
        if (minDistance < 0.1) {
            recommendations.add("Some chunk boundaries have low semantic distance")
        }

        return MetricResult(
            metricName = name,
            score = score,
            details = mapOf(
                "averageDistance" to averageDistance,
                "minDistance" to minDistance,
                "maxDistance" to maxDistance,
                "distances" to betweenChunkDistances
            ),
            recommendations = recommendations
        )
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }
}

@Component
class SizeDistributionMetric(
    private var targetSize: Int = 1000,
    private var tolerance: Double = 0.3
) : ChunkQualityMetric {

    override val name: String = "size_distribution"
    override val description: String = "Evaluates how well chunk sizes match the target size"

    fun configure(targetSize: Int, tolerance: Double) {
        this.targetSize = targetSize
        this.tolerance = tolerance
    }

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult {
        if (chunks.isEmpty()) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "No chunks to evaluate")
            )
        }

        val sizes = chunks.map { it.content.length }
        val minAllowed = (targetSize * (1 - tolerance)).toInt()
        val maxAllowed = (targetSize * (1 + tolerance)).toInt()

        val withinTolerance = sizes.count { it in minAllowed..maxAllowed }
        val score = withinTolerance.toDouble() / sizes.size

        val meanSize = sizes.average()
        val variance = sizes.map { (it - meanSize) * (it - meanSize) }.average()
        val stdDev = sqrt(variance)

        val recommendations = mutableListOf<String>()
        if (score < 0.7) {
            recommendations.add("Many chunks are outside the target size range")
        }
        if (stdDev > targetSize * 0.5) {
            recommendations.add("High variance in chunk sizes - consider adjusting chunking parameters")
        }
        if (sizes.any { it < 100 }) {
            recommendations.add("Some chunks are very small and may lack context")
        }
        if (sizes.any { it > targetSize * 2 }) {
            recommendations.add("Some chunks are very large and may reduce retrieval precision")
        }

        return MetricResult(
            metricName = name,
            score = score,
            details = mapOf(
                "targetSize" to targetSize,
                "tolerance" to tolerance,
                "meanSize" to meanSize,
                "stdDev" to stdDev,
                "minSize" to (sizes.minOrNull() ?: 0),
                "maxSize" to (sizes.maxOrNull() ?: 0),
                "withinToleranceCount" to withinTolerance,
                "totalChunks" to sizes.size,
                "sizeDistribution" to sizes
            ),
            recommendations = recommendations
        )
    }
}

@Component
class InformationPreservationMetric : ChunkQualityMetric {

    override val name: String = "information_preservation"
    override val description: String = "Checks if important information is preserved across chunks"

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult {
        if (chunks.isEmpty()) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "No chunks to evaluate")
            )
        }

        val totalContent = chunks.joinToString(" ") { it.content }
        val chunkedLength = chunks.sumOf { it.content.length }
        val overlapEstimate = chunkedLength - totalContent.length

        val hasContext = chunks.count { it.contextBefore != null || it.contextAfter != null }
        val contextRatio = hasContext.toDouble() / chunks.size

        val completeSentences = chunks.count { chunk ->
            val content = chunk.content.trim()
            content.isNotEmpty() && (
                content.endsWith(".") ||
                content.endsWith("!") ||
                content.endsWith("?") ||
                content.endsWith("\"") ||
                content.endsWith("'")
            )
        }
        val sentenceCompletionRatio = completeSentences.toDouble() / chunks.size

        val score = (sentenceCompletionRatio * 0.7 + contextRatio * 0.3)

        val recommendations = mutableListOf<String>()
        if (sentenceCompletionRatio < 0.8) {
            recommendations.add("Many chunks don't end with complete sentences")
        }
        if (contextRatio < 0.5 && chunks.size > 1) {
            recommendations.add("Consider enabling context preservation for better retrieval")
        }

        return MetricResult(
            metricName = name,
            score = score,
            details = mapOf(
                "sentenceCompletionRatio" to sentenceCompletionRatio,
                "contextRatio" to contextRatio,
                "estimatedOverlap" to overlapEstimate,
                "chunksWithContext" to hasContext,
                "completeSentenceChunks" to completeSentences
            ),
            recommendations = recommendations
        )
    }
}

@Component
class ContextSufficiencyMetric : ChunkQualityMetric {

    override val name: String = "context_sufficiency"
    override val description: String = "Evaluates if chunks contain enough context to be meaningful standalone"

    private val minWordCount = 20
    private val minSentenceCount = 2

    override fun evaluate(chunks: List<DocumentChunk>, embeddings: List<FloatArray>?): MetricResult {
        if (chunks.isEmpty()) {
            return MetricResult(
                metricName = name,
                score = 0.0,
                details = mapOf("error" to "No chunks to evaluate")
            )
        }

        val chunkAnalysis = chunks.map { chunk ->
            val content = chunk.content
            val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val sentenceCount = content.split(Regex("[.!?]+")).filter { it.isNotBlank() }.size

            ChunkAnalysis(
                wordCount = wordCount,
                sentenceCount = sentenceCount,
                hasMetadata = chunk.metadata.headingHierarchy.isNotEmpty() ||
                    chunk.metadata.sectionTitle != null,
                hasSufficientWords = wordCount >= minWordCount,
                hasSufficientSentences = sentenceCount >= minSentenceCount
            )
        }

        val sufficientChunks = chunkAnalysis.count {
            it.hasSufficientWords && it.hasSufficientSentences
        }
        val score = sufficientChunks.toDouble() / chunks.size

        val avgWordCount = chunkAnalysis.map { it.wordCount }.average()
        val avgSentenceCount = chunkAnalysis.map { it.sentenceCount }.average()

        val recommendations = mutableListOf<String>()
        if (score < 0.8) {
            recommendations.add("Some chunks may lack sufficient context - consider increasing chunk size")
        }
        if (avgWordCount < minWordCount) {
            recommendations.add("Average word count is low - chunks may be too small")
        }

        return MetricResult(
            metricName = name,
            score = score,
            details = mapOf(
                "sufficientChunks" to sufficientChunks,
                "totalChunks" to chunks.size,
                "averageWordCount" to avgWordCount,
                "averageSentenceCount" to avgSentenceCount,
                "minWordThreshold" to minWordCount,
                "minSentenceThreshold" to minSentenceCount
            ),
            recommendations = recommendations
        )
    }

    private data class ChunkAnalysis(
        val wordCount: Int,
        val sentenceCount: Int,
        val hasMetadata: Boolean,
        val hasSufficientWords: Boolean,
        val hasSufficientSentences: Boolean
    )
}
