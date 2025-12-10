package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import ai.sovereignrag.commons.chunking.SemanticChunkingStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

private val log = KotlinLogging.logger {}

data class WeightedStrategy(
    val strategy: SemanticChunkingStrategy,
    val weight: Double
)

@Component
class CompositeSemanticChunker(
    private val sentenceAwareStrategy: SentenceAwareChunkingStrategy,
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : ChunkingStrategy {

    override val name: String = "composite-semantic"
    override val description: String = "Combines multiple semantic strategies with weighted voting"

    private val strategies = mutableListOf<WeightedStrategy>()
    private var boundaryThreshold: Double = 0.5

    fun addStrategy(strategy: SemanticChunkingStrategy, weight: Double) {
        strategies.add(WeightedStrategy(strategy, weight))
    }

    fun setStrategies(weightedStrategies: List<WeightedStrategy>) {
        strategies.clear()
        strategies.addAll(weightedStrategies)
    }

    fun setBoundaryThreshold(threshold: Double) {
        boundaryThreshold = threshold
    }

    override fun supports(mimeType: String): Boolean =
        strategies.any { it.strategy.supports(mimeType) }

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        if (strategies.isEmpty()) {
            log.warn { "No strategies configured, falling back to sentence-aware chunking" }
            return sentenceAwareStrategy.chunk(document, config)
        }

        val content = document.content
        if (content.isBlank()) return emptyList()

        val sentences = sentenceAwareStrategy.splitIntoSentences(content, document.language)
        if (sentences.size < 3) {
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

        val boundaryVotes = collectBoundaryVotes(content, sentences)
        val mergedBoundaries = mergeBoundaries(boundaryVotes, sentences.size)

        return createChunksFromBoundaries(document, sentences, mergedBoundaries, config)
    }

    private fun collectBoundaryVotes(
        content: String,
        sentences: List<String>
    ): Map<Int, Double> {
        val votes = mutableMapOf<Int, Double>()
        val totalWeight = strategies.sumOf { it.weight }

        for ((strategy, weight) in strategies) {
            runCatching {
                val boundaries = strategy.detectBoundaries(content, sentences)
                val normalizedWeight = weight / totalWeight

                boundaries.forEach { boundary ->
                    votes[boundary] = (votes[boundary] ?: 0.0) + normalizedWeight
                }
            }.onFailure { e ->
                log.warn(e) { "Strategy ${strategy.name} failed to detect boundaries" }
            }
        }

        return votes
    }

    private fun mergeBoundaries(votes: Map<Int, Double>, totalSentences: Int): List<Int> {
        val boundaries = votes
            .filter { it.value >= boundaryThreshold }
            .keys
            .sorted()

        if (boundaries.isEmpty()) {
            val estimatedChunks = (totalSentences / 5.0).roundToInt().coerceAtLeast(1)
            val step = totalSentences / estimatedChunks
            return (step until totalSentences step step).toList()
        }

        val merged = mutableListOf<Int>()
        var lastBoundary = -3

        for (boundary in boundaries) {
            if (boundary - lastBoundary >= 2) {
                merged.add(boundary)
                lastBoundary = boundary
            }
        }

        return merged
    }

    private fun createChunksFromBoundaries(
        document: Document,
        sentences: List<String>,
        boundaries: List<Int>,
        config: ChunkingConfig
    ): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        val allBoundaries = listOf(0) + boundaries + listOf(sentences.size)
        var currentOffset = 0

        for (i in 0 until allBoundaries.size - 1) {
            val startIdx = allBoundaries[i]
            val endIdx = allBoundaries[i + 1]

            if (endIdx <= startIdx) continue

            val chunkSentences = sentences.subList(startIdx, endIdx)
            val chunkContent = chunkSentences.joinToString(" ").trim()

            if (chunkContent.length < config.minChunkSize && chunks.isNotEmpty()) {
                val lastChunk = chunks.removeLast()
                val mergedContent = "${lastChunk.content} $chunkContent"
                chunks.add(
                    lastChunk.copy(
                        content = mergedContent,
                        endOffset = lastChunk.startOffset + mergedContent.length
                    )
                )
                currentOffset = lastChunk.startOffset + mergedContent.length + 1
                continue
            }

            val contextBefore = if (config.includeContext && startIdx > 0) {
                sentences.subList((startIdx - 2).coerceAtLeast(0), startIdx).joinToString(" ")
            } else null

            val contextAfter = if (config.includeContext && endIdx < sentences.size) {
                sentences.subList(endIdx, (endIdx + 2).coerceAtMost(sentences.size)).joinToString(" ")
            } else null

            chunks.add(
                DocumentChunk(
                    content = chunkContent,
                    index = chunks.size,
                    startOffset = currentOffset,
                    endOffset = currentOffset + chunkContent.length,
                    metadata = ChunkMetadata(
                        sourceType = ContentType.PROSE,
                        language = document.language,
                        strategyUsed = name,
                        additionalMetadata = mapOf(
                            "sentenceCount" to chunkSentences.size,
                            "boundaryConfidence" to (boundaries.find { it == endIdx }?.let {
                                "high"
                            } ?: "estimated")
                        )
                    ),
                    contextBefore = contextBefore,
                    contextAfter = contextAfter
                )
            )

            currentOffset += chunkContent.length + 1
        }

        return splitOversizedChunks(chunks, config, document)
    }

    private fun splitOversizedChunks(
        chunks: List<DocumentChunk>,
        config: ChunkingConfig,
        document: Document
    ): List<DocumentChunk> {
        val result = mutableListOf<DocumentChunk>()
        var globalIndex = 0

        for (chunk in chunks) {
            if (chunk.content.length <= config.maxChunkSize) {
                result.add(chunk.copy(index = globalIndex))
                globalIndex++
            } else {
                val subChunks = sentenceAwareStrategy.chunk(
                    Document(
                        content = chunk.content,
                        mimeType = "text/plain",
                        language = document.language
                    ),
                    config
                )

                subChunks.forEach { subChunk ->
                    result.add(
                        subChunk.copy(
                            index = globalIndex,
                            startOffset = chunk.startOffset + subChunk.startOffset,
                            endOffset = chunk.startOffset + subChunk.endOffset,
                            metadata = chunk.metadata.copy(
                                strategyUsed = "$name+${sentenceAwareStrategy.name}"
                            )
                        )
                    )
                    globalIndex++
                }
            }
        }

        return result
    }
}
