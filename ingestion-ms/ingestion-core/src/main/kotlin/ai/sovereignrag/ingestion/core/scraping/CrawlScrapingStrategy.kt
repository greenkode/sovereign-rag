package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
class CrawlScrapingStrategy(
    private val browserManager: PlaywrightBrowserManager
) : ScrapingStrategy {

    override fun supports(mode: ScrapeMode): Boolean = mode == ScrapeMode.CRAWL

    override fun scrape(
        job: IngestionJob,
        url: String,
        metadata: WebScrapeJobMetadata,
        progressCallback: (Int) -> Unit
    ): List<ScrapedPage> {
        log.info { "Starting crawl from: $url with maxDepth=${metadata.maxDepth}, maxPages=${metadata.maxPages}" }

        val visited = mutableSetOf<String>()
        val toVisit = mutableListOf(Pair(url, 0))
        val pages = mutableListOf<ScrapedPage>()
        val baseHost = URI.create(url).host

        val page = browserManager.getOrCreateBrowser().newPage()
        try {
            while (toVisit.isNotEmpty() && pages.size < metadata.maxPages) {
                val (currentUrl, depth) = toVisit.removeFirst()

                if (currentUrl in visited) continue
                visited.add(currentUrl)

                if (!ScrapingUtils.shouldProcessUrl(currentUrl, metadata)) {
                    log.debug { "Skipping URL due to filter: $currentUrl" }
                    continue
                }

                runCatching {
                    page.navigate(currentUrl)
                    page.waitForLoadState()

                    val title = page.title()
                    val content = browserManager.extractContent(page)

                    pages.add(ScrapedPage(url = currentUrl, title = title, content = content, depth = depth))

                    if (depth < metadata.maxDepth) {
                        extractLinks(page, currentUrl, baseHost, metadata, visited).forEach { link ->
                            toVisit.add(Pair(link, depth + 1))
                        }
                    }

                    Thread.sleep(metadata.delayBetweenRequestsMs)

                    val progress = ((pages.size.toFloat() / metadata.maxPages) * 60 + 10).toInt()
                    progressCallback(progress)

                }.onFailure { e ->
                    log.warn { "Failed to scrape $currentUrl: ${e.message}" }
                }
            }
        } finally {
            page.close()
        }

        log.info { "Crawl completed: scraped ${pages.size} pages" }
        return pages
    }

    private fun extractLinks(
        page: com.microsoft.playwright.Page,
        currentUrl: String,
        baseHost: String,
        metadata: WebScrapeJobMetadata,
        visited: Set<String>
    ): List<String> {
        return page.locator("a[href]").all()
            .mapNotNull { link -> link.getAttribute("href") }
            .filter { it.isNotBlank() }
            .map { ScrapingUtils.resolveUrl(currentUrl, it) }
            .filter { resolvedUrl ->
                resolvedUrl !in visited &&
                    (metadata.followExternalLinks || runCatching { URI.create(resolvedUrl).host }.getOrNull() == baseHost)
            }
            .distinct()
    }
}
