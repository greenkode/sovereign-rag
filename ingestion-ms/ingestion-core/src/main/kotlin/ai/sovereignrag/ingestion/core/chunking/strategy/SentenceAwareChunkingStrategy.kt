package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import org.springframework.stereotype.Component
import java.text.BreakIterator
import java.util.Locale

@Component
class SentenceAwareChunkingStrategy(
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : ChunkingStrategy {

    override val name: String = "sentence-aware"
    override val description: String = "Chunks text while respecting sentence boundaries"

    override fun supports(mimeType: String): Boolean =
        mimeType.startsWith("text/") || mimeType == "application/pdf"

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val sentences = splitIntoSentences(content, document.language)
        if (sentences.isEmpty()) return emptyList()

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
        var currentChunk = StringBuilder()
        var currentStartOffset = 0
        var chunkStartOffset = 0
        var index = 0

        for (sentence in sentences) {
            val trimmedSentence = sentence.trim()
            if (trimmedSentence.isEmpty()) {
                currentStartOffset += sentence.length
                continue
            }

            if (currentChunk.isEmpty()) {
                chunkStartOffset = currentStartOffset
            }

            val potentialLength = currentChunk.length + (if (currentChunk.isNotEmpty()) 1 else 0) + trimmedSentence.length

            if (potentialLength > config.chunkSize && currentChunk.isNotEmpty()) {
                val chunkContent = currentChunk.toString()
                if (chunkContent.length >= config.minChunkSize) {
                    chunks.add(
                        DocumentChunk(
                            content = chunkContent,
                            index = index,
                            startOffset = chunkStartOffset,
                            endOffset = chunkStartOffset + chunkContent.length,
                            metadata = ChunkMetadata(
                                sourceType = ContentType.PROSE,
                                language = document.language,
                                strategyUsed = name
                            )
                        )
                    )
                    index++
                }

                val overlapSentences = getOverlapSentences(chunks.lastOrNull()?.content ?: "", config.chunkOverlap)
                currentChunk = StringBuilder()
                if (overlapSentences.isNotEmpty()) {
                    currentChunk.append(overlapSentences)
                    chunkStartOffset = currentStartOffset - overlapSentences.length
                } else {
                    chunkStartOffset = currentStartOffset
                }
            }

            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(trimmedSentence)
            currentStartOffset += sentence.length
        }

        if (currentChunk.isNotEmpty()) {
            val chunkContent = currentChunk.toString()
            if (chunkContent.length >= config.minChunkSize) {
                chunks.add(
                    DocumentChunk(
                        content = chunkContent,
                        index = index,
                        startOffset = chunkStartOffset,
                        endOffset = chunkStartOffset + chunkContent.length,
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

    fun splitIntoSentences(text: String, language: String = "en"): List<String> {
        val locale = Locale.forLanguageTag(language)
        val iterator = BreakIterator.getSentenceInstance(locale)
        iterator.setText(text)

        val sentences = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end)
            if (sentence.isNotBlank()) {
                sentences.add(sentence)
            }
            start = end
            end = iterator.next()
        }

        return sentences
    }

    private fun getOverlapSentences(text: String, targetOverlap: Int): String {
        if (text.length <= targetOverlap) return text

        val sentences = splitIntoSentences(text)
        if (sentences.isEmpty()) return ""

        val result = StringBuilder()
        var totalLength = 0

        for (sentence in sentences.reversed()) {
            val sentenceWithSpace = if (result.isEmpty()) sentence.trim() else "${sentence.trim()} "
            if (totalLength + sentenceWithSpace.length > targetOverlap && result.isNotEmpty()) {
                break
            }
            result.insert(0, sentenceWithSpace)
            totalLength += sentenceWithSpace.length
        }

        return result.toString().trim()
    }
}
