package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SitemapScrapingStrategy(
    private val browserManager: PlaywrightBrowserManager,
    private val sitemapParser: SitemapParser
) : ScrapingStrategy {

    override fun supports(mode: ScrapeMode): Boolean = mode == ScrapeMode.SITEMAP

    override fun scrape(
        job: IngestionJob,
        url: String,
        metadata: WebScrapeJobMetadata,
        progressCallback: (Int) -> Unit
    ): List<ScrapedPage> {
        val sitemapUrl = metadata.sitemapUrl
            ?: throw IllegalStateException("Sitemap URL is required for SITEMAP mode")

        log.info { "Scraping from sitemap: $sitemapUrl" }

        val sitemapEntries = sitemapParser.parse(sitemapUrl, metadata.priorityThreshold)
        val urlsToScrape = sitemapEntries
            .filter { ScrapingUtils.shouldProcessUrl(it.loc, metadata) }
            .take(metadata.maxPages)
            .map { it.loc }

        log.info { "Found ${urlsToScrape.size} URLs to scrape from sitemap" }

        val pages = mutableListOf<ScrapedPage>()
        val playwrightPage = browserManager.getOrCreateBrowser().newPage()

        try {
            urlsToScrape.forEachIndexed { index, pageUrl ->
                runCatching {
                    playwrightPage.navigate(pageUrl)
                    playwrightPage.waitForLoadState()

                    val title = playwrightPage.title()
                    val content = browserManager.extractContent(playwrightPage)

                    pages.add(ScrapedPage(url = pageUrl, title = title, content = content))

                    Thread.sleep(metadata.delayBetweenRequestsMs)

                    val progress = ((index + 1).toFloat() / urlsToScrape.size * 60 + 10).toInt()
                    progressCallback(progress)

                }.onFailure { e ->
                    log.warn { "Failed to scrape $pageUrl: ${e.message}" }
                }
            }
        } finally {
            playwrightPage.close()
        }

        log.info { "Sitemap scraping completed: scraped ${pages.size} pages" }
        return pages
    }
}
