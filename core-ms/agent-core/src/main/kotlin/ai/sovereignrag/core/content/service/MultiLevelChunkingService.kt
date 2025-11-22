package ai.sovereignrag.core.content.service

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service to create multi-level document chunks for enhanced retrieval
 *
 * Three levels:
 * - Micro: Key facts and entities (200-300 chars)
 * - Standard: Regular chunks (current implementation, 800-1200 chars)
 * - Macro: Section/document summaries (3000-5000 chars)
 */
@Service
class MultiLevelChunkingService(
    private val chatLanguageModel: ChatLanguageModel
) {

    /**
     * Extract micro chunks - key facts and entities
     */
    fun extractKeyFacts(content: String, title: String, maxFacts: Int = 10): List<String> {
        if (content.length < 100) {
            return emptyList()
        }

        try {
            val prompt = """
Extract the ${maxFacts} most important facts or key points from this content.

Title: $title
Content: ${content.take(2000)}

Return each fact as a single, concise sentence (max 50 words each).
Format as a numbered list.
            """.trimIndent()

            val response = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            // Parse numbered list
            return response.lines()
                .filter { it.matches(Regex("^\\d+\\.\\s+.+")) }
                .map { it.replaceFirst(Regex("^\\d+\\.\\s+"), "").trim() }
                .filter { it.length in 20..300 }
                .take(maxFacts)

        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract key facts, falling back to simple extraction" }
            // Fallback: Extract first few sentences
            return content.split(Regex("[.!?]\\s+"))
                .filter { it.length in 50..300 }
                .take(5)
        }
    }

    /**
     * Generate macro chunk - document summary
     */
    fun generateDocumentSummary(content: String, title: String, targetLength: Int = 500): String {
        if (content.length < 200) {
            return content
        }

        try {
            val prompt = """
Summarize this document in approximately $targetLength characters.

Title: $title
Content: $content

Requirements:
- Capture the main topic and key points
- Keep it informative and specific
- Target length: $targetLength characters
- Write in paragraph form
            """.trimIndent()

            val summary = chatLanguageModel.generate(UserMessage.from(prompt)).content().text()

            return summary.take(targetLength + 200) // Allow some flexibility

        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate summary, using excerpt" }
            // Fallback: Return first portion of content
            return content.take(targetLength)
        }
    }

    /**
     * Create all chunk levels for a document
     */
    fun createMultiLevelChunks(
        content: String,
        title: String,
        standardChunks: List<String>
    ): MultiLevelChunks {
        logger.debug { "Creating multi-level chunks for: $title" }

        val microChunks = if (content.length > 300) {
            extractKeyFacts(content, title)
        } else emptyList()

        val macroChunk = if (content.length > 1000) {
            generateDocumentSummary(content, title)
        } else null

        return MultiLevelChunks(
            micro = microChunks,
            standard = standardChunks,
            macro = macroChunk
        )
    }
}

/**
 * Container for multi-level chunks
 */
data class MultiLevelChunks(
    val micro: List<String>,      // Key facts (200-300 chars each)
    val standard: List<String>,    // Regular chunks (800-1200 chars)
    val macro: String?              // Document summary (3000-5000 chars)
)
