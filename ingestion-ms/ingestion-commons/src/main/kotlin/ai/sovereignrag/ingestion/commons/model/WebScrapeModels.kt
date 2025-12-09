package ai.sovereignrag.ingestion.commons.model

import java.time.Instant

enum class ScrapeMode {
    SINGLE,
    CRAWL,
    SITEMAP
}

data class WebScrapeJobMetadata(
    val mode: ScrapeMode = ScrapeMode.SINGLE,
    val maxPages: Int = 10,
    val delayBetweenRequestsMs: Long = 1000,
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = emptyList(),
    val maxDepth: Int = 2,
    val followExternalLinks: Boolean = false,
    val sitemapUrl: String? = null,
    val priorityThreshold: Double = 0.0
)

data class SitemapEntry(
    val loc: String,
    val lastmod: Instant? = null,
    val changefreq: String? = null,
    val priority: Double? = null
)

data class ScrapedPage(
    val url: String,
    val title: String,
    val content: String,
    val depth: Int = 0
)
