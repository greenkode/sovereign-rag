package ai.sovereignrag.ingestion.core.rss

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class RssFeedParser {

    fun parse(feedUrl: String, maxItems: Int = 50): ParsedFeed {
        log.info { "Parsing RSS/Atom feed: $feedUrl" }

        val uri = URI.create(feedUrl)
        val input = SyndFeedInput()

        XmlReader(uri.toURL()).use { reader ->
            val feed = input.build(reader)
            val entries = feed.entries
                .take(maxItems)
                .mapNotNull { entry -> parseEntry(entry, feed) }

            log.info { "Parsed ${entries.size} entries from feed: ${feed.title}" }

            return ParsedFeed(
                title = feed.title ?: feedUrl,
                description = feed.description,
                link = feed.link,
                feedType = feed.feedType,
                entries = entries
            )
        }
    }

    private fun parseEntry(entry: SyndEntry, feed: SyndFeed): FeedEntry? {
        val title = entry.title
        val content = extractContent(entry)

        if (title.isNullOrBlank() && content.isBlank()) {
            log.debug { "Skipping entry with no title or content" }
            return null
        }

        return FeedEntry(
            title = title ?: "Untitled",
            link = entry.link,
            content = content,
            description = entry.description?.value,
            author = entry.author,
            publishedDate = entry.publishedDate?.toInstant(),
            updatedDate = entry.updatedDate?.toInstant(),
            categories = entry.categories?.mapNotNull { it.name } ?: emptyList()
        )
    }

    private fun extractContent(entry: SyndEntry): String {
        val contents = entry.contents
        if (contents.isNotEmpty()) {
            return contents
                .mapNotNull { it.value }
                .joinToString("\n\n")
                .let { stripHtml(it) }
        }

        return entry.description?.value?.let { stripHtml(it) } ?: ""
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

data class ParsedFeed(
    val title: String,
    val description: String?,
    val link: String?,
    val feedType: String?,
    val entries: List<FeedEntry>
)

data class FeedEntry(
    val title: String,
    val link: String?,
    val content: String,
    val description: String?,
    val author: String?,
    val publishedDate: Instant?,
    val updatedDate: Instant?,
    val categories: List<String>
)
