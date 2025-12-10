package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import org.springframework.stereotype.Component

@Component
class RecursiveCharacterSplitter(
    private val defaultConfig: ChunkingConfig = ChunkingConfig(),
    private val separators: List<String> = DEFAULT_SEPARATORS
) : ChunkingStrategy {

    companion object {
        val DEFAULT_SEPARATORS = listOf(
            "\n\n\n",
            "\n\n",
            "\n",
            ". ",
            "! ",
            "? ",
            "; ",
            ", ",
            " ",
            ""
        )

        val MARKDOWN_SEPARATORS = listOf(
            "\n## ",
            "\n### ",
            "\n#### ",
            "\n##### ",
            "\n###### ",
            "\n\n",
            "\n",
            ". ",
            " ",
            ""
        )

        val CODE_SEPARATORS = listOf(
            "\n\nclass ",
            "\n\nfun ",
            "\n\ndef ",
            "\n\nfunction ",
            "\n\n",
            "\n",
            " ",
            ""
        )
    }

    override val name: String = "recursive-character"
    override val description: String = "Recursively splits text using a hierarchy of separators"

    override fun supports(mimeType: String): Boolean = true

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig, separators)

    fun chunk(
        document: Document,
        config: ChunkingConfig,
        customSeparators: List<String> = separators
    ): List<DocumentChunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val splits = splitText(content, customSeparators, config.chunkSize)
        return mergeSplits(splits, config, document)
    }

    private fun splitText(
        text: String,
        separators: List<String>,
        chunkSize: Int
    ): List<String> {
        val finalChunks = mutableListOf<String>()

        var separator = separators.lastOrNull() ?: ""
        var newSeparators = emptyList<String>()

        for (i in separators.indices) {
            val sep = separators[i]
            if (sep.isEmpty() || text.contains(sep)) {
                separator = sep
                newSeparators = separators.subList(i + 1, separators.size)
                break
            }
        }

        val splits = if (separator.isEmpty()) {
            text.toList().map { it.toString() }
        } else {
            splitWithSeparator(text, separator)
        }

        val goodSplits = mutableListOf<String>()

        for (split in splits) {
            if (split.length < chunkSize) {
                goodSplits.add(split)
            } else {
                if (goodSplits.isNotEmpty()) {
                    val mergedText = mergeSplitsSimple(goodSplits, separator)
                    finalChunks.addAll(splitIfNeeded(mergedText, chunkSize))
                    goodSplits.clear()
                }

                if (newSeparators.isEmpty()) {
                    finalChunks.addAll(splitIfNeeded(split, chunkSize))
                } else {
                    val subSplits = splitText(split, newSeparators, chunkSize)
                    finalChunks.addAll(subSplits)
                }
            }
        }

        if (goodSplits.isNotEmpty()) {
            val mergedText = mergeSplitsSimple(goodSplits, separator)
            finalChunks.addAll(splitIfNeeded(mergedText, chunkSize))
        }

        return finalChunks
    }

    private fun splitWithSeparator(text: String, separator: String): List<String> {
        val parts = text.split(separator)
        val result = mutableListOf<String>()

        for (i in parts.indices) {
            val part = parts[i]
            if (i < parts.size - 1) {
                result.add(part + separator)
            } else if (part.isNotEmpty()) {
                result.add(part)
            }
        }

        return result
    }

    private fun mergeSplitsSimple(splits: List<String>, separator: String): String =
        splits.joinToString(if (separator.isBlank()) "" else "")

    private fun splitIfNeeded(text: String, chunkSize: Int): List<String> {
        if (text.length <= chunkSize) return listOf(text)

        val result = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = (start + chunkSize).coerceAtMost(text.length)
            result.add(text.substring(start, end))
            start = end
        }

        return result
    }

    private fun mergeSplits(
        splits: List<String>,
        config: ChunkingConfig,
        document: Document
    ): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        var currentChunk = StringBuilder()
        var currentStartOffset = 0
        var chunkStartOffset = 0
        var index = 0

        for (split in splits) {
            if (split.isBlank()) {
                currentStartOffset += split.length
                continue
            }

            if (currentChunk.isEmpty()) {
                chunkStartOffset = currentStartOffset
            }

            val potentialLength = currentChunk.length + split.length

            if (potentialLength > config.chunkSize && currentChunk.isNotEmpty()) {
                val chunkContent = currentChunk.toString().trim()
                if (chunkContent.length >= config.minChunkSize) {
                    chunks.add(
                        DocumentChunk(
                            content = chunkContent,
                            index = index,
                            startOffset = chunkStartOffset,
                            endOffset = chunkStartOffset + currentChunk.length,
                            metadata = ChunkMetadata(
                                sourceType = ContentType.PROSE,
                                language = document.language,
                                strategyUsed = name
                            )
                        )
                    )
                    index++
                }

                currentChunk = StringBuilder()
                val overlap = getOverlapContent(chunks.lastOrNull()?.content ?: "", config.chunkOverlap)
                if (overlap.isNotEmpty()) {
                    currentChunk.append(overlap)
                }
                chunkStartOffset = currentStartOffset
            }

            currentChunk.append(split)
            currentStartOffset += split.length
        }

        if (currentChunk.isNotEmpty()) {
            val chunkContent = currentChunk.toString().trim()
            if (chunkContent.length >= config.minChunkSize) {
                chunks.add(
                    DocumentChunk(
                        content = chunkContent,
                        index = index,
                        startOffset = chunkStartOffset,
                        endOffset = chunkStartOffset + currentChunk.length,
                        metadata = ChunkMetadata(
                            sourceType = ContentType.PROSE,
                            language = document.language,
                            strategyUsed = name
                        )
                    )
                )
            }
        }

        return chunks
    }

    private fun getOverlapContent(text: String, targetOverlap: Int): String {
        if (text.length <= targetOverlap) return text
        return text.takeLast(targetOverlap)
    }
}
