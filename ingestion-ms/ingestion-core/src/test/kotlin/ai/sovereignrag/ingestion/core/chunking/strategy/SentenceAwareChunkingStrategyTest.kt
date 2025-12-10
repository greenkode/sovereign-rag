package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SentenceAwareChunkingStrategyTest {

    private lateinit var strategy: SentenceAwareChunkingStrategy

    @BeforeEach
    fun setup() {
        strategy = SentenceAwareChunkingStrategy()
    }

    @Test
    fun `should split text into sentences`() {
        val text = "First sentence. Second sentence! Third sentence? Fourth sentence."

        val sentences = strategy.splitIntoSentences(text)

        assertEquals(4, sentences.size)
    }

    @Test
    fun `should handle abbreviations in text`() {
        val text = "Dr. Smith went to the U.S.A. yesterday. He met Mr. Jones there."

        val sentences = strategy.splitIntoSentences(text)

        assertTrue(sentences.size >= 2)
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
    fun `should group sentences into chunks respecting size limit`() {
        val sentences = (1..20).map { "This is sentence number $it." }
        val content = sentences.joinToString(" ")
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 200, chunkOverlap = 50, minChunkSize = 50)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(chunk.content.length <= 300) // Allow some margin
        }
    }

    @Test
    fun `should preserve complete sentences in chunks`() {
        val content = "First sentence here. Second sentence follows. Third one now. Fourth sentence ends."
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 50, chunkOverlap = 10, minChunkSize = 20)

        val chunks = strategy.chunk(document, config)

        chunks.forEach { chunk ->
            val trimmed = chunk.content.trim()
            assertTrue(
                trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?"),
                "Chunk should end with sentence boundary: $trimmed"
            )
        }
    }

    @Test
    fun `should handle empty content`() {
        val document = Document(content = "", mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `should handle content with only whitespace`() {
        val document = Document(content = "   \n\t  ", mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `should set correct metadata`() {
        val document = Document(
            content = "Test content for metadata.",
            mimeType = "text/plain",
            language = "en"
        )
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertEquals(1, chunks.size)
        assertEquals("sentence-aware", chunks[0].metadata.strategyUsed)
        assertEquals("en", chunks[0].metadata.language)
    }

    @Test
    fun `should support text mime types`() {
        assertTrue(strategy.supports("text/plain"))
        assertTrue(strategy.supports("text/html"))
        assertTrue(strategy.supports("text/markdown"))
        assertTrue(strategy.supports("application/pdf"))
    }

    @Test
    fun `should handle different languages`() {
        val document = Document(
            content = "Dies ist ein deutscher Satz. Hier ist noch einer.",
            mimeType = "text/plain",
            language = "de"
        )
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertEquals(1, chunks.size)
        assertTrue(chunks[0].content.contains("deutscher"))
    }

    @Test
    fun `should include overlap between chunks`() {
        val sentences = (1..10).map { "Sentence number $it is here." }
        val content = sentences.joinToString(" ")
        val document = Document(content = content, mimeType = "text/plain")
        val config = ChunkingConfig(chunkSize = 100, chunkOverlap = 30, minChunkSize = 30)

        val chunks = strategy.chunk(document, config)

        if (chunks.size > 1) {
            for (i in 0 until chunks.size - 1) {
                val currentEnd = chunks[i].content.takeLast(30)
                val nextStart = chunks[i + 1].content.take(50)
                val hasOverlap = currentEnd.split(" ").any { word ->
                    word.isNotBlank() && nextStart.contains(word)
                }
                assertTrue(hasOverlap || chunks[i + 1].content.length < 50)
            }
        }
    }
}
