package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkMetadata
import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ChunkingStrategy
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import ai.sovereignrag.commons.chunking.DocumentChunk
import org.springframework.stereotype.Component

@Component
class MarkdownChunkingStrategy(
    private val defaultConfig: ChunkingConfig = ChunkingConfig()
) : ChunkingStrategy {

    override val name: String = "markdown"
    override val description: String = "Chunks markdown content while preserving structure"

    private val headingPattern = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
    private val codeBlockPattern = Regex("```[\\s\\S]*?```", RegexOption.MULTILINE)
    private val inlineCodePattern = Regex("`[^`]+`")
    private val listItemPattern = Regex("^\\s*[-*+]\\s+|^\\s*\\d+\\.\\s+", RegexOption.MULTILINE)

    override fun supports(mimeType: String): Boolean =
        mimeType == "text/markdown" || mimeType == "text/x-markdown"

    override fun chunk(document: Document): List<DocumentChunk> =
        chunk(document, defaultConfig)

    fun chunk(document: Document, config: ChunkingConfig): List<DocumentChunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val sections = splitByHeadings(content)
        return sections.flatMapIndexed { sectionIndex, section ->
            chunkSection(section, config, document, sectionIndex)
        }
    }

    private fun splitByHeadings(content: String): List<MarkdownSection> {
        val sections = mutableListOf<MarkdownSection>()
        val headings = headingPattern.findAll(content).toList()

        if (headings.isEmpty()) {
            sections.add(
                MarkdownSection(
                    headingHierarchy = emptyList(),
                    content = content,
                    startOffset = 0,
                    endOffset = content.length
                )
            )
            return sections
        }

        val currentHierarchy = mutableListOf<String>()

        if (headings.first().range.first > 0) {
            sections.add(
                MarkdownSection(
                    headingHierarchy = emptyList(),
                    content = content.substring(0, headings.first().range.first).trim(),
                    startOffset = 0,
                    endOffset = headings.first().range.first
                )
            )
        }

        for (i in headings.indices) {
            val heading = headings[i]
            val level = heading.groupValues[1].length
            val title = heading.groupValues[2].trim()

            while (currentHierarchy.size >= level) {
                currentHierarchy.removeAt(currentHierarchy.size - 1)
            }
            currentHierarchy.add(title)

            val startOffset = heading.range.last + 1
            val endOffset = if (i < headings.size - 1) {
                headings[i + 1].range.first
            } else {
                content.length
            }

            val sectionContent = if (startOffset < endOffset) {
                content.substring(startOffset, endOffset).trim()
            } else ""

            if (sectionContent.isNotBlank()) {
                sections.add(
                    MarkdownSection(
                        headingHierarchy = currentHierarchy.toList(),
                        content = sectionContent,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        level = level
                    )
                )
            }
        }

        return sections
    }

    private fun chunkSection(
        section: MarkdownSection,
        config: ChunkingConfig,
        document: Document,
        sectionIndex: Int
    ): List<DocumentChunk> {
        val content = section.content
        if (content.length <= config.chunkSize) {
            return listOf(
                DocumentChunk(
                    content = content,
                    index = sectionIndex,
                    startOffset = section.startOffset,
                    endOffset = section.endOffset,
                    metadata = ChunkMetadata(
                        sourceType = determineContentType(content),
                        headingHierarchy = section.headingHierarchy,
                        sectionTitle = section.headingHierarchy.lastOrNull(),
                        language = document.language,
                        strategyUsed = name
                    )
                )
            )
        }

        val preservedBlocks = extractPreservedBlocks(content)
        val processedContent = replacePreservedBlocks(content, preservedBlocks)

        val paragraphs = splitIntoParagraphs(processedContent)
        val chunks = mutableListOf<DocumentChunk>()
        var currentChunk = StringBuilder()
        var chunkIndex = 0
        var currentOffset = section.startOffset

        for (paragraph in paragraphs) {
            val restoredParagraph = restorePreservedBlocks(paragraph, preservedBlocks)

            if (currentChunk.length + restoredParagraph.length > config.chunkSize && currentChunk.isNotEmpty()) {
                chunks.add(
                    DocumentChunk(
                        content = currentChunk.toString().trim(),
                        index = sectionIndex * 1000 + chunkIndex,
                        startOffset = currentOffset,
                        endOffset = currentOffset + currentChunk.length,
                        metadata = ChunkMetadata(
                            sourceType = determineContentType(currentChunk.toString()),
                            headingHierarchy = section.headingHierarchy,
                            sectionTitle = section.headingHierarchy.lastOrNull(),
                            language = document.language,
                            strategyUsed = name
                        )
                    )
                )
                chunkIndex++
                currentOffset += currentChunk.length
                currentChunk = StringBuilder()
            }

            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(restoredParagraph)
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(
                DocumentChunk(
                    content = currentChunk.toString().trim(),
                    index = sectionIndex * 1000 + chunkIndex,
                    startOffset = currentOffset,
                    endOffset = currentOffset + currentChunk.length,
                    metadata = ChunkMetadata(
                        sourceType = determineContentType(currentChunk.toString()),
                        headingHierarchy = section.headingHierarchy,
                        sectionTitle = section.headingHierarchy.lastOrNull(),
                        language = document.language,
                        strategyUsed = name
                    )
                )
            )
        }

        return chunks
    }

    private fun extractPreservedBlocks(content: String): Map<String, String> {
        val blocks = mutableMapOf<String, String>()
        var index = 0

        codeBlockPattern.findAll(content).forEach { match ->
            val placeholder = "<<CODE_BLOCK_$index>>"
            blocks[placeholder] = match.value
            index++
        }

        return blocks
    }

    private fun replacePreservedBlocks(content: String, blocks: Map<String, String>): String {
        var result = content
        blocks.forEach { (placeholder, original) ->
            result = result.replace(original, placeholder)
        }
        return result
    }

    private fun restorePreservedBlocks(content: String, blocks: Map<String, String>): String {
        var result = content
        blocks.forEach { (placeholder, original) ->
            result = result.replace(placeholder, original)
        }
        return result
    }

    private fun splitIntoParagraphs(content: String): List<String> =
        content.split(Regex("\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun determineContentType(content: String): ContentType = when {
        codeBlockPattern.containsMatchIn(content) -> ContentType.CODE
        listItemPattern.containsMatchIn(content) -> ContentType.LIST
        content.startsWith("|") && content.contains("---") -> ContentType.TABLE
        content.startsWith(">") -> ContentType.QUOTE
        else -> ContentType.PROSE
    }

    private data class MarkdownSection(
        val headingHierarchy: List<String>,
        val content: String,
        val startOffset: Int,
        val endOffset: Int,
        val level: Int = 0
    )
}
