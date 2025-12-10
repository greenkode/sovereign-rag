package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import java.net.URI
import java.util.regex.Pattern

object ScrapingUtils {

    fun shouldProcessUrl(url: String, metadata: WebScrapeJobMetadata): Boolean {
        val includeMatches = metadata.includePatterns.isEmpty() ||
            metadata.includePatterns.any { pattern ->
                Pattern.compile(pattern).matcher(url).find()
            }

        val excludeMatches = metadata.excludePatterns.any { pattern ->
            Pattern.compile(pattern).matcher(url).find()
        }

        return includeMatches && !excludeMatches
    }

    fun resolveUrl(baseUrl: String, href: String): String {
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> {
                val uri = URI.create(baseUrl)
                "${uri.scheme}://${uri.host}$href"
            }
            else -> {
                val uri = URI.create(baseUrl)
                val basePath = uri.path.substringBeforeLast('/')
                "${uri.scheme}://${uri.host}$basePath/$href"
            }
        }
    }

    fun chunkContent(content: String, chunkSize: Int, overlap: Int): List<String> {
        if (content.length <= chunkSize) {
            return listOf(content)
        }

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < content.length) {
            val end = (start + chunkSize).coerceAtMost(content.length)
            val chunk = content.substring(start, end)
            chunks.add(chunk)

            start = end - overlap
            if (start >= content.length - overlap) break
        }

        return chunks
    }
}
