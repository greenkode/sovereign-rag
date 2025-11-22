package ai.sovereignrag.core.content.loader

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * Service for loading documents from various formats
 *
 * Supports:
 * - Plain text
 * - HTML
 * - PDF
 * - Word (DOC, DOCX)
 * - And many more via Apache Tika
 */
@Service
class DocumentLoaderService {

    private val textParser: DocumentParser = TextDocumentParser()
    private val tikaParser: DocumentParser = ApacheTikaDocumentParser()

    /**
     * Load document from text content
     */
    fun loadFromText(
        content: String,
        metadata: Map<String, Any> = emptyMap()
    ): Document {
        logger.debug { "Loading document from text (${content.length} chars)" }
        return Document.from(content, Metadata.from(metadata))
    }

    /**
     * Load document from HTML content
     */
    fun loadFromHtml(
        html: String,
        metadata: Map<String, Any> = emptyMap()
    ): Document {
        logger.debug { "Loading document from HTML" }
        // Simple HTML stripping - Tika can do better for complex HTML
        val cleanText = html
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Document.from(cleanText, Metadata.from(metadata))
    }

    /**
     * Load document from file path
     *
     * Automatically detects file type and uses appropriate parser
     */
    fun loadFromFile(
        path: Path,
        metadata: Map<String, Any> = emptyMap()
    ): Document {
        logger.info { "Loading document from file: $path" }

        return when (path.toString().substringAfterLast('.').lowercase()) {
            "txt", "text" -> {
                textParser.parse(path.toFile().inputStream())
            }
            else -> {
                // Use Tika for all other formats (PDF, DOC, DOCX, HTML, etc.)
                tikaParser.parse(path.toFile().inputStream())
            }
        }.apply {
            // Merge with provided metadata
            metadata.forEach { (key, value) ->
                when (value) {
                    is String -> metadata().put(key, value)
                    is Int -> metadata().put(key, value)
                    is Long -> metadata().put(key, value)
                    is Float -> metadata().put(key, value)
                    is Double -> metadata().put(key, value)
                    else -> metadata().put(key, value.toString())
                }
            }
        }
    }

    /**
     * Load document from input stream
     *
     * @param inputStream Input stream
     * @param fileName Optional file name for type detection
     * @param metadata Additional metadata
     */
    fun loadFromInputStream(
        inputStream: InputStream,
        fileName: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): Document {
        logger.info { "Loading document from input stream (fileName: $fileName)" }

        val parser = if (fileName != null && fileName.endsWith(".txt", ignoreCase = true)) {
            textParser
        } else {
            tikaParser
        }

        return parser.parse(inputStream).apply {
            // Merge with provided metadata
            metadata.forEach { (key, value) ->
                when (value) {
                    is String -> metadata().put(key, value)
                    is Int -> metadata().put(key, value)
                    is Long -> metadata().put(key, value)
                    is Float -> metadata().put(key, value)
                    is Double -> metadata().put(key, value)
                    else -> metadata().put(key, value.toString())
                }
            }
        }
    }

    /**
     * Load document from URL (useful for web scraping)
     * Note: This is a placeholder - full implementation would need HTTP client
     */
    fun loadFromUrl(
        url: String,
        metadata: Map<String, Any> = emptyMap()
    ): Document {
        logger.info { "Loading document from URL: $url" }
        // This would need implementation with HTTP client
        throw UnsupportedOperationException("URL loading not yet implemented")
    }
}
