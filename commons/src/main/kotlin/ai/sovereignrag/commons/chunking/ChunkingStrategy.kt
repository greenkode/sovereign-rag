package ai.sovereignrag.commons.chunking

import java.util.UUID

interface ChunkingStrategy {
    val name: String
    val description: String

    fun chunk(document: Document): List<DocumentChunk>
    fun supports(mimeType: String): Boolean
}

interface SemanticChunkingStrategy : ChunkingStrategy {
    fun detectBoundaries(content: String, sentences: List<String>): List<Int>
}

data class Document(
    val id: UUID = UUID.randomUUID(),
    val content: String,
    val mimeType: String,
    val metadata: Map<String, Any> = emptyMap(),
    val structure: DocumentStructure? = null,
    val language: String = "en"
)

data class DocumentStructure(
    val headings: List<Heading> = emptyList(),
    val paragraphs: List<TextBlock> = emptyList(),
    val codeBlocks: List<CodeBlock> = emptyList(),
    val tables: List<TableBlock> = emptyList(),
    val lists: List<ListBlock> = emptyList()
)

data class Heading(
    val level: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

data class TextBlock(
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

data class CodeBlock(
    val content: String,
    val language: String?,
    val startOffset: Int,
    val endOffset: Int
)

data class TableBlock(
    val content: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val startOffset: Int,
    val endOffset: Int
)

data class ListBlock(
    val items: List<String>,
    val ordered: Boolean,
    val startOffset: Int,
    val endOffset: Int
)

data class DocumentChunk(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata,
    val parentChunkId: String? = null,
    val childChunkIds: List<String> = emptyList(),
    val contextBefore: String? = null,
    val contextAfter: String? = null
)

data class ChunkMetadata(
    val sourceId: UUID? = null,
    val sourceType: ContentType = ContentType.PROSE,
    val headingHierarchy: List<String> = emptyList(),
    val sectionTitle: String? = null,
    val language: String = "en",
    val confidence: Double = 1.0,
    val strategyUsed: String? = null,
    val additionalMetadata: Map<String, Any> = emptyMap()
)

enum class ContentType {
    PROSE,
    CODE,
    TABLE,
    LIST,
    HEADER,
    QUOTE,
    METADATA,
    MIXED
}

data class ChunkingConfig(
    val chunkSize: Int = 1000,
    val chunkOverlap: Int = 200,
    val minChunkSize: Int = 100,
    val maxChunkSize: Int = 2000,
    val preserveSentences: Boolean = true,
    val preserveParagraphs: Boolean = true,
    val includeContext: Boolean = false,
    val contextSize: Int = 100
)
