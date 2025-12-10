package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.SemanticChunkingStrategy
import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.ingestion.core.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.sqrt

private val log = KotlinLogging.logger {}

@Component
class BreakpointSemanticChunker(
    private val embeddingService: EmbeddingService,
    private val sentenceAwareStrategy: SentenceAwareChunkingStrategy,
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : SemanticChunkingStrategy {

    override val name: String = "semantic-breakpoint"
    override val description: String = "Detects semantic boundaries using embedding similarity"

    var similarityThreshold: Double = 0.5
    var percentileBreakpoint: Int = 95
    var minSentencesPerChunk: Int = 2
    var maxSentencesPerChunk: Int = 20

    override fun supports(mimeType: String): Boolean =
        mimeType.startsWith("text/") || mimeType == "application/pdf"

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig, null)

    fun chunk(
        document: Document,
        config: ChunkingConfig,
        modelConfig: EmbeddingModelConfig?
    ): List<DocumentChunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val sentences = splitIntoSentences(content, document.language)
        if (sentences.size <= minSentencesPerChunk) {
            return listOf(
                DocumentChunk(
                    content = content,
                    index = 0,
                    startOffset = 0,
                    endOffset = content.length,
                    metadata = ChunkMetadata(
                        sourceType = ContentType.PROSE,
                        language = document.language,
                        strategyUsed = name,
                        confidence = 1.0
                    )
                )
            )
        }

        return modelConfig?.let { mc ->
            chunkWithEmbeddings(document, sentences, config, mc)
        } ?: chunkWithoutEmbeddings(document, config)
    }

    private fun chunkWithEmbeddings(
        document: Document,
        sentences: List<String>,
        config: ChunkingConfig,
        modelConfig: EmbeddingModelConfig
    ): List<DocumentChunk> {
        log.debug { "Generating embeddings for ${sentences.size} sentences" }

        val embeddings = runCatching {
            embeddingService.generateEmbeddings(sentences, modelConfig)
        }.getOrElse { e ->
            log.warn(e) { "Failed to generate embeddings, falling back to sentence-aware chunking" }
            return chunkWithoutEmbeddings(document, config)
        }

        val distances = computeAdjacentDistances(embeddings)
        val breakpoints = findBreakpoints(distances)

        return createChunksFromBreakpoints(document, sentences, breakpoints, config)
    }

    private fun chunkWithoutEmbeddings(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        log.debug { "Using sentence-aware fallback for semantic chunking" }
        return sentenceAwareStrategy.chunk(document, config).map { chunk ->
            chunk.copy(
                metadata = chunk.metadata.copy(
                    strategyUsed = "$name-fallback",
                    confidence = 0.7
                )
            )
        }
    }

    override fun detectBoundaries(content: String, sentences: List<String>): List<Int> {
        if (sentences.size < 3) return emptyList()

        return listOf(sentences.size / 2)
    }

    fun detectBoundaries(
        sentences: List<String>,
        modelConfig: EmbeddingModelConfig
    ): List<Int> {
        if (sentences.size < 3) return emptyList()

        val embeddings = embeddingService.generateEmbeddings(sentences, modelConfig)
        val distances = computeAdjacentDistances(embeddings)

        return findBreakpoints(distances)
    }

    private fun computeAdjacentDistances(embeddings: List<FloatArray>): List<Double> {
        if (embeddings.size < 2) return emptyList()

        return embeddings.zipWithNext { a, b ->
            1.0 - cosineSimilarity(a, b)
        }
    }

    private fun findBreakpoints(distances: List<Double>): List<Int> {
        if (distances.isEmpty()) return emptyList()

        val threshold = computeThreshold(distances)
        val breakpoints = mutableListOf<Int>()

        for (i in distances.indices) {
            if (distances[i] >= threshold) {
                if (breakpoints.isEmpty() || i - breakpoints.last() >= minSentencesPerChunk) {
                    breakpoints.add(i + 1)
                }
            }
        }

        return breakpoints
    }

    private fun computeThreshold(distances: List<Double>): Double {
        val sorted = distances.sorted()
        val index = ((percentileBreakpoint / 100.0) * sorted.size).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index].coerceAtLeast(similarityThreshold)
    }

    private fun createChunksFromBreakpoints(
        document: Document,
        sentences: List<String>,
        breakpoints: List<Int>,
        config: ChunkingConfig
    ): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        var startIdx = 0
        var currentOffset = 0
        var chunkIndex = 0

        val allBreakpoints = breakpoints + listOf(sentences.size)

        for (breakpoint in allBreakpoints) {
            if (breakpoint <= startIdx) continue

            var endIdx = breakpoint.coerceAtMost(startIdx + maxSentencesPerChunk)

            while (endIdx > startIdx) {
                val chunkSentences = sentences.subList(startIdx, endIdx)
                val chunkContent = chunkSentences.joinToString(" ").trim()

                if (chunkContent.length <= config.maxChunkSize) {
                    val contextBefore = if (config.includeContext && startIdx > 0) {
                        sentences.subList((startIdx - 2).coerceAtLeast(0), startIdx).joinToString(" ")
                    } else null

                    val contextAfter = if (config.includeContext && endIdx < sentences.size) {
                        sentences.subList(endIdx, (endIdx + 2).coerceAtMost(sentences.size)).joinToString(" ")
                    } else null

                    chunks.add(
                        DocumentChunk(
                            content = chunkContent,
                            index = chunkIndex,
                            startOffset = currentOffset,
                            endOffset = currentOffset + chunkContent.length,
                            metadata = ChunkMetadata(
                                sourceType = ContentType.PROSE,
                                language = document.language,
                                strategyUsed = name,
                                confidence = 0.9
                            ),
                            contextBefore = contextBefore,
                            contextAfter = contextAfter
                        )
                    )
                    chunkIndex++
                    currentOffset += chunkContent.length + 1
                    startIdx = endIdx
                    break
                }

                endIdx--
            }
        }

        return chunks
    }

    private fun splitIntoSentences(text: String, language: String): List<String> =
        sentenceAwareStrategy.splitIntoSentences(text, language)

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
class SlidingWindowSemanticChunker(
    private val embeddingService: EmbeddingService,
    private val sentenceAwareStrategy: SentenceAwareChunkingStrategy,
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : SemanticChunkingStrategy {

    override val name: String = "semantic-sliding-window"
    override val description: String = "Uses sliding window with context preservation"

    var windowSize: Int = 5
    var stepSize: Int = 3
    var contextSentences: Int = 1

    override fun supports(mimeType: String): Boolean =
        mimeType.startsWith("text/") || mimeType == "application/pdf"

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val sentences = sentenceAwareStrategy.splitIntoSentences(content, document.language)
        if (sentences.size <= windowSize) {
            return listOf(
                DocumentChunk(
                    content = content,
                    index = 0,
                    startOffset = 0,
                    endOffset = content.length,
                    metadata = ChunkMetadata(
                        sourceType = ContentType.PROSE,
                        language = document.language,
                        strategyUsed = name
                    )
                )
            )
        }

        val chunks = mutableListOf<DocumentChunk>()
        var currentOffset = 0
        var index = 0

        var windowStart = 0
        while (windowStart < sentences.size) {
            val windowEnd = (windowStart + windowSize).coerceAtMost(sentences.size)
            val windowSentences = sentences.subList(windowStart, windowEnd)
            val chunkContent = windowSentences.joinToString(" ").trim()

            val contextBefore = if (windowStart > 0) {
                sentences.subList((windowStart - contextSentences).coerceAtLeast(0), windowStart)
                    .joinToString(" ")
            } else null

            val contextAfter = if (windowEnd < sentences.size) {
                sentences.subList(windowEnd, (windowEnd + contextSentences).coerceAtMost(sentences.size))
                    .joinToString(" ")
            } else null

            chunks.add(
                DocumentChunk(
                    content = chunkContent,
                    index = index,
                    startOffset = currentOffset,
                    endOffset = currentOffset + chunkContent.length,
                    metadata = ChunkMetadata(
                        sourceType = ContentType.PROSE,
                        language = document.language,
                        strategyUsed = name
                    ),
                    contextBefore = contextBefore,
                    contextAfter = contextAfter
                )
            )

            index++
            currentOffset += chunkContent.length + 1
            windowStart += stepSize

            if (windowStart + stepSize >= sentences.size && windowStart < sentences.size) {
                windowStart = sentences.size - windowSize
                if (windowStart <= chunks.lastOrNull()?.index?.let { sentences.size - windowSize } ?: -1) {
                    break
                }
            }
        }

        return chunks
    }

    override fun detectBoundaries(content: String, sentences: List<String>): List<Int> {
        val boundaries = mutableListOf<Int>()
        var position = stepSize

        while (position < sentences.size) {
            boundaries.add(position)
            position += stepSize
        }

        return boundaries
    }
}
