package ai.sovereignrag.ingestion.core.chunking

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.QualityReport
import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.ingestion.core.chunking.quality.DefaultChunkQualityEvaluator
import ai.sovereignrag.ingestion.core.chunking.strategy.BreakpointSemanticChunker
import ai.sovereignrag.ingestion.core.chunking.strategy.CompositeSemanticChunker
import ai.sovereignrag.ingestion.core.chunking.strategy.FixedSizeChunkingStrategy
import ai.sovereignrag.ingestion.core.chunking.strategy.MarkdownChunkingStrategy
import ai.sovereignrag.ingestion.core.chunking.strategy.RecursiveCharacterSplitter
import ai.sovereignrag.ingestion.core.chunking.strategy.SentenceAwareChunkingStrategy
import ai.sovereignrag.ingestion.core.chunking.strategy.SlidingWindowSemanticChunker
import ai.sovereignrag.ingestion.core.chunking.strategy.WeightedStrategy
import ai.sovereignrag.ingestion.core.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class ChunkingService(
    private val contentTypeRouter: ContentTypeRouter,
    private val fixedSizeStrategy: FixedSizeChunkingStrategy,
    private val sentenceAwareStrategy: SentenceAwareChunkingStrategy,
    private val recursiveStrategy: RecursiveCharacterSplitter,
    private val markdownStrategy: MarkdownChunkingStrategy,
    private val compositeStrategy: CompositeSemanticChunker,
    private val breakpointSemanticChunker: BreakpointSemanticChunker,
    private val slidingWindowSemanticChunker: SlidingWindowSemanticChunker,
    private val qualityEvaluator: DefaultChunkQualityEvaluator,
    private val embeddingService: EmbeddingService
) {
    private var defaultConfig = ChunkingConfig()
    private var enableQualityEvaluation = true
    private var useSemanticChunking = false

    fun setDefaultConfig(config: ChunkingConfig) {
        defaultConfig = config
    }

    fun enableQualityEvaluation(enabled: Boolean) {
        enableQualityEvaluation = enabled
    }

    fun enableSemanticChunking(enabled: Boolean) {
        useSemanticChunking = enabled
    }

    fun chunk(
        content: String,
        mimeType: String,
        config: ChunkingConfig = defaultConfig,
        sourceId: UUID? = null,
        modelConfig: EmbeddingModelConfig? = null
    ): ChunkingResult {
        val document = Document(
            content = content,
            mimeType = mimeType,
            metadata = sourceId?.let { mapOf("sourceId" to it.toString()) } ?: emptyMap()
        )

        return chunk(document, config, modelConfig)
    }

    fun chunk(
        document: Document,
        config: ChunkingConfig = defaultConfig,
        modelConfig: EmbeddingModelConfig? = null
    ): ChunkingResult {
        log.debug { "Chunking document with MIME type: ${document.mimeType}, size: ${document.content.length}" }

        val strategy = selectStrategy(document.mimeType, modelConfig)
        val startTime = System.currentTimeMillis()

        val chunks = when {
            useSemanticChunking && modelConfig != null -> {
                chunkWithSemanticStrategy(document, config, modelConfig)
            }
            else -> {
                chunkWithStrategy(strategy, document, config)
            }
        }

        val processingTimeMs = System.currentTimeMillis() - startTime
        log.debug { "Chunking completed: ${chunks.size} chunks in ${processingTimeMs}ms" }

        val qualityReport = if (enableQualityEvaluation && chunks.isNotEmpty()) {
            evaluateQuality(chunks, modelConfig)
        } else null

        return ChunkingResult(
            chunks = chunks,
            strategyUsed = strategy.name,
            processingTimeMs = processingTimeMs,
            qualityReport = qualityReport
        )
    }

    fun chunkWithStrategy(
        strategyName: String,
        document: Document,
        config: ChunkingConfig = defaultConfig
    ): ChunkingResult {
        val strategy = getStrategy(strategyName)
        val startTime = System.currentTimeMillis()

        val chunks = chunkWithStrategy(strategy, document, config)
        val processingTimeMs = System.currentTimeMillis() - startTime

        val qualityReport = if (enableQualityEvaluation && chunks.isNotEmpty()) {
            evaluateQuality(chunks, null)
        } else null

        return ChunkingResult(
            chunks = chunks,
            strategyUsed = strategyName,
            processingTimeMs = processingTimeMs,
            qualityReport = qualityReport
        )
    }

    private fun chunkWithStrategy(
        strategy: ChunkingStrategy,
        document: Document,
        config: ChunkingConfig
    ): List<DocumentChunk> = when (strategy) {
        is FixedSizeChunkingStrategy -> strategy.chunk(document, config)
        is SentenceAwareChunkingStrategy -> strategy.chunk(document, config)
        is RecursiveCharacterSplitter -> strategy.chunk(document, config)
        is MarkdownChunkingStrategy -> strategy.chunk(document, config)
        is CompositeSemanticChunker -> strategy.chunk(document, config)
        is ContentTypeRouter -> strategy.chunk(document, config)
        else -> strategy.chunk(document)
    }

    private fun chunkWithSemanticStrategy(
        document: Document,
        config: ChunkingConfig,
        modelConfig: EmbeddingModelConfig
    ): List<DocumentChunk> {
        log.debug { "Using semantic chunking with model: ${modelConfig.modelId}" }
        return breakpointSemanticChunker.chunk(document, config, modelConfig)
    }

    private fun selectStrategy(mimeType: String, modelConfig: EmbeddingModelConfig?): ChunkingStrategy {
        if (useSemanticChunking && modelConfig != null) {
            return breakpointSemanticChunker
        }
        return contentTypeRouter.selectStrategy(mimeType)
    }

    private fun getStrategy(name: String): ChunkingStrategy = when (name) {
        "fixed-size" -> fixedSizeStrategy
        "sentence-aware" -> sentenceAwareStrategy
        "recursive-character" -> recursiveStrategy
        "markdown" -> markdownStrategy
        "composite-semantic" -> compositeStrategy
        "semantic-breakpoint" -> breakpointSemanticChunker
        "semantic-sliding-window" -> slidingWindowSemanticChunker
        "content-type-router" -> contentTypeRouter
        else -> throw IllegalArgumentException("Unknown strategy: $name")
    }

    private fun evaluateQuality(
        chunks: List<DocumentChunk>,
        modelConfig: EmbeddingModelConfig?
    ): QualityReport {
        val embeddings = modelConfig?.let {
            runCatching {
                embeddingService.generateEmbeddings(chunks.map { c -> c.content }, it)
            }.getOrNull()
        }

        return qualityEvaluator.evaluate(chunks, embeddings)
    }

    fun configureCompositeStrategy(strategies: List<Pair<String, Double>>) {
        val weightedStrategies = strategies.mapNotNull { (name, weight) ->
            when (name) {
                "semantic-breakpoint" -> WeightedStrategy(breakpointSemanticChunker, weight)
                "semantic-sliding-window" -> WeightedStrategy(slidingWindowSemanticChunker, weight)
                else -> null
            }
        }
        compositeStrategy.setStrategies(weightedStrategies)
    }

    fun getAvailableStrategies(): List<StrategyInfo> = listOf(
        StrategyInfo("fixed-size", fixedSizeStrategy.description, listOf("*")),
        StrategyInfo("sentence-aware", sentenceAwareStrategy.description, listOf("text/*", "application/pdf")),
        StrategyInfo("recursive-character", recursiveStrategy.description, listOf("*")),
        StrategyInfo("markdown", markdownStrategy.description, listOf("text/markdown")),
        StrategyInfo("composite-semantic", compositeStrategy.description, listOf("text/*")),
        StrategyInfo("semantic-breakpoint", breakpointSemanticChunker.description, listOf("text/*")),
        StrategyInfo("semantic-sliding-window", slidingWindowSemanticChunker.description, listOf("text/*")),
        StrategyInfo("content-type-router", contentTypeRouter.description, listOf("*"))
    )
}

data class ChunkingResult(
    val chunks: List<DocumentChunk>,
    val strategyUsed: String,
    val processingTimeMs: Long,
    val qualityReport: QualityReport?
)

data class StrategyInfo(
    val name: String,
    val description: String,
    val supportedMimeTypes: List<String>
)
