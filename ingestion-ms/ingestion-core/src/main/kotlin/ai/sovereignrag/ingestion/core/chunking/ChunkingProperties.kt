package ai.sovereignrag.ingestion.core.chunking

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.QualityThresholds
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ingestion.chunking")
data class ChunkingProperties(
    val defaultStrategy: String = "content-type-router",
    val enableQualityEvaluation: Boolean = true,
    val enableSemanticChunking: Boolean = false,

    val fixedSize: FixedSizeProperties = FixedSizeProperties(),
    val sentenceAware: SentenceAwareProperties = SentenceAwareProperties(),
    val semantic: SemanticProperties = SemanticProperties(),
    val markdown: MarkdownProperties = MarkdownProperties(),
    val composite: CompositeProperties = CompositeProperties(),
    val quality: QualityProperties = QualityProperties(),

    val contentTypeMapping: Map<String, String> = emptyMap()
) {
    fun toChunkingConfig(): ChunkingConfig = ChunkingConfig(
        chunkSize = fixedSize.chunkSize,
        chunkOverlap = fixedSize.overlap,
        minChunkSize = fixedSize.minChunkSize,
        maxChunkSize = fixedSize.maxChunkSize,
        preserveSentences = sentenceAware.preserveSentences,
        preserveParagraphs = sentenceAware.preserveParagraphs,
        includeContext = semantic.includeContext,
        contextSize = semantic.contextSize
    )

    fun toQualityThresholds(): QualityThresholds = QualityThresholds(
        minimumOverallScore = quality.minimumOverallScore,
        minimumCoherenceScore = quality.minimumCoherenceScore,
        minimumBoundaryScore = quality.minimumBoundaryScore,
        targetChunkSize = fixedSize.chunkSize,
        chunkSizeTolerance = quality.chunkSizeTolerance
    )
}

data class FixedSizeProperties(
    val chunkSize: Int = 1000,
    val overlap: Int = 200,
    val minChunkSize: Int = 100,
    val maxChunkSize: Int = 2000
)

data class SentenceAwareProperties(
    val maxSentencesPerChunk: Int = 10,
    val preserveSentences: Boolean = true,
    val preserveParagraphs: Boolean = true
)

data class SemanticProperties(
    val similarityThreshold: Double = 0.5,
    val percentileBreakpoint: Int = 95,
    val minSentencesPerChunk: Int = 2,
    val maxSentencesPerChunk: Int = 20,
    val windowSize: Int = 5,
    val stepSize: Int = 3,
    val includeContext: Boolean = true,
    val contextSize: Int = 100
)

data class MarkdownProperties(
    val preserveCodeBlocks: Boolean = true,
    val preserveTables: Boolean = true,
    val headingAsContext: Boolean = true
)

data class CompositeProperties(
    val strategies: List<WeightedStrategyConfig> = listOf(
        WeightedStrategyConfig("semantic-breakpoint", 0.6),
        WeightedStrategyConfig("semantic-sliding-window", 0.4)
    ),
    val boundaryThreshold: Double = 0.5
)

data class WeightedStrategyConfig(
    val name: String,
    val weight: Double
)

data class QualityProperties(
    val enabled: Boolean = true,
    val minimumOverallScore: Double = 0.7,
    val minimumCoherenceScore: Double = 0.6,
    val minimumBoundaryScore: Double = 0.5,
    val chunkSizeTolerance: Double = 0.3,
    val metrics: List<String> = listOf(
        "internal_coherence",
        "boundary_quality",
        "size_distribution",
        "information_preservation",
        "context_sufficiency"
    )
)
