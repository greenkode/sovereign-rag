package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FixedSizeChunkingStrategyTest {

    private lateinit var strategy: FixedSizeChunkingStrategy

    @BeforeEach
    fun setup() {
        strategy = FixedSizeChunkingStrategy()
    }

    @Test
    fun `should return single chunk for small content`() {
        val document = Document(
            content = "This is a small document.",
            mimeType = "text/plain"
        )
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertEquals(1, chunks.size)
        assertEquals("This is a small document.", chunks[0].content)
    }

    @Test
    fun `should split content into multiple chunks with overlap`() {
        val content = "A".repeat(500) + ". " + "B".repeat(500) + ". " + "C".repeat(500)
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 600, chunkOverlap = 100, minChunkSize = 50)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 700) // chunkSize + some tolerance for sentence boundaries
        }
    }

    @Test
    fun `should respect sentence boundaries when preserveSentences is true`() {
        val content = "First sentence. Second sentence. Third sentence. Fourth sentence. Fifth sentence."
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 50, chunkOverlap = 10, minChunkSize = 10, preserveSentences = true)

        val chunks = strategy.chunk(document, config)

        chunks.forEach { chunk ->
            val trimmed = chunk.content.trim()
            assertTrue(
                trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?") || chunk == chunks.last(),
                "Chunk should end with sentence boundary: $trimmed"
            )
        }
    }

    @Test
    fun `should include context when configured`() {
        val content = "First part of the document. Second part of the document. Third part of the document."
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(
            chunkSize = 30,
            chunkOverlap = 5,
            minChunkSize = 10,
            includeContext = true,
            contextSize = 20
        )

        val chunks = strategy.chunk(document, config)

        val middleChunks = chunks.filter { it.index > 0 && it.index < chunks.size - 1 }
        middleChunks.forEach { chunk ->
            assertTrue(chunk.contextBefore != null || chunk.contextAfter != null)
        }
    }

    @Test
    fun `should set correct metadata`() {
        val document = Document(
            content = "Test content for metadata verification.",
            mimeType = "text/plain",
            language = "en"
        )
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertEquals(1, chunks.size)
        assertEquals("fixed-size", chunks[0].metadata.strategyUsed)
        assertEquals("en", chunks[0].metadata.language)
    }

    @Test
    fun `should handle empty content`() {
        val document = Document(content = "", mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isEmpty() || (chunks.size == 1 && chunks[0].content.isEmpty()))
    }

    @Test
    fun `should support all mime types`() {
        assertTrue(strategy.supports("text/plain"))
        assertTrue(strategy.supports("application/pdf"))
        assertTrue(strategy.supports("application/json"))
        assertTrue(strategy.supports("image/png"))
    }

    @Test
    fun `should set correct offsets`() {
        val content = "First chunk content. Second chunk content. Third chunk content."
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 25, chunkOverlap = 5, minChunkSize = 10)

        val chunks = strategy.chunk(document, config)

        chunks.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.index)
            assertTrue(chunk.startOffset >= 0)
            assertTrue(chunk.endOffset > chunk.startOffset)
        }
    }
}
