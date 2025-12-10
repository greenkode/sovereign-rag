package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import org.springframework.stereotype.Component

@Component
class FixedSizeChunkingStrategy(
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : ChunkingStrategy {

    override val name: String = "fixed-size"
    override val description: String = "Simple fixed-size chunking with configurable overlap"

    override fun supports(mimeType: String): Boolean = true

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        val content = document.content
        if (content.length <= config.chunkSize) {
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
        var start = 0
        var index = 0

        while (start < content.length) {
            val end = (start + config.chunkSize).coerceAtMost(content.length)
            var chunkEnd = end

            if (config.preserveSentences && end < content.length) {
                chunkEnd = findSentenceBoundary(content, start, end, config.chunkSize)
            }

            val chunkContent = content.substring(start, chunkEnd)

            if (chunkContent.length >= config.minChunkSize) {
                val contextBefore = if (config.includeContext && start > 0) {
                    val contextStart = (start - config.contextSize).coerceAtLeast(0)
                    content.substring(contextStart, start)
                } else null

                val contextAfter = if (config.includeContext && chunkEnd < content.length) {
                    val contextEnd = (chunkEnd + config.contextSize).coerceAtMost(content.length)
                    content.substring(chunkEnd, contextEnd)
                } else null

                chunks.add(
                    DocumentChunk(
                        content = chunkContent,
                        index = index,
                        startOffset = start,
                        endOffset = chunkEnd,
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
            }

            val nextStart = chunkEnd - config.chunkOverlap
            start = if (nextStart <= start) chunkEnd else nextStart

            if (start >= content.length - config.minChunkSize) break
        }

        return chunks
    }

    private fun findSentenceBoundary(content: String, start: Int, end: Int, maxSize: Int): Int {
        val searchStart = (end - 100).coerceAtLeast(start)
        val searchRegion = content.substring(searchStart, end)

        val sentenceEnders = listOf(". ", "! ", "? ", ".\n", "!\n", "?\n")
        var bestBoundary = end

        for (ender in sentenceEnders) {
            val lastIndex = searchRegion.lastIndexOf(ender)
            if (lastIndex != -1) {
                val absolutePosition = searchStart + lastIndex + ender.length
                if (absolutePosition > start + (maxSize / 2)) {
                    bestBoundary = absolutePosition
                    break
                }
            }
        }

        return bestBoundary.coerceAtMost(content.length)
    }
}
