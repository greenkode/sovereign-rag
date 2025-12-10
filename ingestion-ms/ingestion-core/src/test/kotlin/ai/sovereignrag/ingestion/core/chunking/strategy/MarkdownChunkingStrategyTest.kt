package ai.sovereignrag.ingestion.core.chunking.strategy

import ai.sovereignrag.commons.chunking.ChunkingConfig
import ai.sovereignrag.commons.chunking.ContentType
import ai.sovereignrag.commons.chunking.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MarkdownChunkingStrategyTest {

    private lateinit var strategy: MarkdownChunkingStrategy

    @BeforeEach
    fun setup() {
        strategy = MarkdownChunkingStrategy()
    }

    @Test
    fun `should split markdown by headings`() {
        val content = """
            # Introduction
            This is the introduction section.

            ## Getting Started
            Here's how to get started.

            ## Installation
            Follow these installation steps.
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.size >= 2)
    }

    @Test
    fun `should preserve heading hierarchy in metadata`() {
        val content = """
            # Main Title
            Introduction text.

            ## Chapter 1
            Chapter content here.

            ### Section 1.1
            Section content.
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        val sectionChunk = chunks.find { it.content.contains("Section content") }
        assertTrue(sectionChunk != null)
        assertTrue(sectionChunk!!.metadata.headingHierarchy.isNotEmpty())
    }

    @Test
    fun `should preserve code blocks`() {
        val content = """
            # Code Example

            Here's some code:

            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```

            That was the code.
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        val codeChunk = chunks.find { it.content.contains("println") }
        assertTrue(codeChunk != null)
        assertTrue(codeChunk!!.content.contains("```"))
    }

    @Test
    fun `should identify code content type`() {
        val content = """
            # Code Section

            ```python
            def hello():
                print("Hello")
            ```
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        val codeChunk = chunks.find { it.content.contains("def hello") }
        assertTrue(codeChunk != null)
        assertEquals(ContentType.CODE, codeChunk!!.metadata.sourceType)
    }

    @Test
    fun `should identify list content type`() {
        val content = """
            # Shopping List

            - Apples
            - Bananas
            - Oranges
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        val listChunk = chunks.find { it.content.contains("Apples") }
        assertTrue(listChunk != null)
        assertEquals(ContentType.LIST, listChunk!!.metadata.sourceType)
    }

    @Test
    fun `should handle content without headings`() {
        val content = "This is plain text without any headings. Just regular content."

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        assertEquals(1, chunks.size)
        assertEquals(content, chunks[0].content)
    }

    @Test
    fun `should handle empty content`() {
        val document = Document(content = "", mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 1000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `should support markdown mime types`() {
        assertTrue(strategy.supports("text/markdown"))
        assertTrue(strategy.supports("text/x-markdown"))
    }

    @Test
    fun `should not support non-markdown mime types`() {
        assertTrue(!strategy.supports("text/plain"))
        assertTrue(!strategy.supports("application/json"))
    }

    @Test
    fun `should set strategy name in metadata`() {
        val content = "# Test\nContent here."
        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isNotEmpty())
        assertEquals("markdown", chunks[0].metadata.strategyUsed)
    }

    @Test
    fun `should handle nested headings correctly`() {
        val content = """
            # Level 1
            Content for level 1.

            ## Level 2
            Content for level 2.

            ### Level 3
            Content for level 3.

            #### Level 4
            Content for level 4.

            ##### Level 5
            Content for level 5.

            ###### Level 6
            Content for level 6.
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 2000)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.size >= 6)
    }

    @Test
    fun `should split large sections into smaller chunks`() {
        val longContent = "This is a very long paragraph. ".repeat(100)
        val content = """
            # Long Section

            $longContent
        """.trimIndent()

        val document = Document(content = content, mimeType = "text/markdown")
        val config = ChunkingConfig(chunkSize = 500, minChunkSize = 100)

        val chunks = strategy.chunk(document, config)

        assertTrue(chunks.isNotEmpty())
    }
}
