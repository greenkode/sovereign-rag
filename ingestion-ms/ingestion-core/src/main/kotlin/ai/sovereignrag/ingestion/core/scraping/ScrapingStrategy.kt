package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata

interface ScrapingStrategy {

    fun supports(mode: ScrapeMode): Boolean

    fun scrape(
        job: IngestionJob,
        url: String,
        metadata: WebScrapeJobMetadata,
        progressCallback: (Int) -> Unit
    ): List<ScrapedPage>
}
