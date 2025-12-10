package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SinglePageScrapingStrategy(
    private val browserManager: PlaywrightBrowserManager
) : ScrapingStrategy {

    override fun supports(mode: ScrapeMode): Boolean = mode == ScrapeMode.SINGLE

    override fun scrape(
        job: IngestionJob,
        url: String,
        metadata: WebScrapeJobMetadata,
        progressCallback: (Int) -> Unit
    ): List<ScrapedPage> {
        log.info { "Scraping single page: $url" }

        if (!ScrapingUtils.shouldProcessUrl(url, metadata)) {
            log.debug { "Skipping URL due to filter: $url" }
            return emptyList()
        }

        progressCallback(30)

        val page = browserManager.withPage { page ->
            page.navigate(url)
            page.waitForLoadState()

            val title = page.title()
            val content = browserManager.extractContent(page)

            ScrapedPage(url = url, title = title, content = content, depth = 0)
        }

        progressCallback(70)

        return listOf(page)
    }
}
