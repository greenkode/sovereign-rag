package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.JobType
import ai.sovereignrag.ingestion.commons.model.ScrapedPage
import ai.sovereignrag.ingestion.commons.model.ScrapeMode
import ai.sovereignrag.ingestion.commons.model.WebScrapeJobMetadata
import ai.sovereignrag.ingestion.commons.queue.JobQueue
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.scraping.SitemapParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.net.URI
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

@Component
class WebScrapeProcessor(
    private val jobRepository: IngestionJobRepository,
    private val jobQueue: JobQueue,
    private val knowledgeSourceGateway: KnowledgeSourceGateway,
    private val sitemapParser: SitemapParser,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper
) {
    private val lock = ReentrantLock()
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    private fun getOrCreateBrowser(): Browser {
        browser?.takeIf { it.isConnected }?.let { return it }

        return lock.withLock {
            browser?.takeIf { it.isConnected }?.let { return it }

            cleanup()

            log.info { "Initializing Playwright browser..." }
            val pw = Playwright.create()
            playwright = pw

            val launchOptions = BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(listOf(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu"
                ))

            val br = pw.chromium().launch(launchOptions)
            browser = br
            log.info { "Playwright browser initialized successfully" }
            br
        }
    }

    @PreDestroy
    fun cleanup() {
        lock.withLock {
            runCatching { browser?.close() }
                .onFailure { log.warn { "Error closing browser: ${it.message}" } }
            runCatching { playwright?.close() }
                .onFailure { log.warn { "Error closing playwright: ${it.message}" } }
            browser = null
            playwright = null
        }
    }

    fun process(job: IngestionJob) {
        log.info { "Processing web scrape job ${job.id}: ${job.sourceReference}" }

        val url = job.sourceReference
            ?: throw IllegalStateException("No URL for web scrape job ${job.id}")

        val metadata = parseMetadata(job)

        updateProgress(job, 10)

        val scrapedPages = when (metadata.mode) {
            ScrapeMode.SINGLE -> scrapeSingle(url, metadata)
            ScrapeMode.CRAWL -> crawlAndScrape(url, metadata, job)
            ScrapeMode.SITEMAP -> scrapeFromSitemap(metadata, job)
        }

        updateProgress(job, 70)

        job.knowledgeBaseId?.let { kbId ->
            processScrapedPages(job, kbId, scrapedPages)
        } ?: log.warn { "No knowledge base ID for job ${job.id}, skipping knowledge source creation" }

        val totalContent = scrapedPages.sumOf { it.content.length }
        job.markCompleted(
            chunksCreated = 0,
            bytesProcessed = totalContent.toLong()
        )
        jobRepository.save(job)

        log.info { "Completed web scrape job ${job.id}: scraped ${scrapedPages.size} pages" }
    }

    private fun parseMetadata(job: IngestionJob): WebScrapeJobMetadata {
        return job.metadata?.let {
            runCatching { objectMapper.readValue<WebScrapeJobMetadata>(it) }
                .getOrElse { parseLegacyMetadata(job.metadata!!) }
        } ?: WebScrapeJobMetadata()
    }

    private fun parseLegacyMetadata(metadata: String): WebScrapeJobMetadata {
        val map = objectMapper.readValue<Map<String, Any>>(metadata)
        val crawl = map["crawl"] as? Boolean ?: false
        val maxDepth = (map["maxDepth"] as? Number)?.toInt() ?: 2
        val maxPages = (map["maxPages"] as? Number)?.toInt() ?: 10

        return WebScrapeJobMetadata(
            mode = crawl.takeIf { it }?.let { ScrapeMode.CRAWL } ?: ScrapeMode.SINGLE,
            maxDepth = maxDepth,
            maxPages = maxPages
        )
    }

    private fun scrapeSingle(url: String, metadata: WebScrapeJobMetadata): List<ScrapedPage> {
        return shouldProcessUrl(url, metadata).takeIf { it }?.let {
            listOf(scrapePage(url, 0))
        } ?: emptyList()
    }

    private fun crawlAndScrape(
        startUrl: String,
        metadata: WebScrapeJobMetadata,
        job: IngestionJob
    ): List<ScrapedPage> {
        val visited = mutableSetOf<String>()
        val toVisit = mutableListOf(Pair(startUrl, 0))
        val pages = mutableListOf<ScrapedPage>()
        val baseHost = URI.create(startUrl).host

        val page = getOrCreateBrowser().newPage()
        try {
            while (toVisit.isNotEmpty() && pages.size < metadata.maxPages) {
                val (url, depth) = toVisit.removeFirst()

                if (url in visited) continue
                visited.add(url)

                if (!shouldProcessUrl(url, metadata)) {
                    log.debug { "Skipping URL due to filter: $url" }
                    continue
                }

                runCatching {
                    page.navigate(url)
                    page.waitForLoadState()

                    val title = page.title()
                    val content = extractContent(page)

                    pages.add(ScrapedPage(url = url, title = title, content = content, depth = depth))

                    if (depth < metadata.maxDepth) {
                        extractLinks(page, url, baseHost, metadata, visited).forEach { link ->
                            toVisit.add(Pair(link, depth + 1))
                        }
                    }

                    Thread.sleep(metadata.delayBetweenRequestsMs)

                    val progress = ((pages.size.toFloat() / metadata.maxPages) * 60 + 10).toInt()
                    updateProgress(job, progress)

                }.onFailure { e ->
                    log.warn { "Failed to scrape $url: ${e.message}" }
                }
            }
        } finally {
            page.close()
        }

        return pages
    }

    private fun scrapeFromSitemap(
        metadata: WebScrapeJobMetadata,
        job: IngestionJob
    ): List<ScrapedPage> {
        val sitemapUrl = metadata.sitemapUrl
            ?: throw IllegalStateException("Sitemap URL is required for SITEMAP mode")

        val sitemapEntries = sitemapParser.parse(sitemapUrl, metadata.priorityThreshold)
        val urlsToScrape = sitemapEntries
            .filter { shouldProcessUrl(it.loc, metadata) }
            .take(metadata.maxPages)
            .map { it.loc }

        log.info { "Scraping ${urlsToScrape.size} URLs from sitemap" }

        val pages = mutableListOf<ScrapedPage>()
        val playwrightPage = getOrCreateBrowser().newPage()

        try {
            urlsToScrape.forEachIndexed { index, url ->
                runCatching {
                    playwrightPage.navigate(url)
                    playwrightPage.waitForLoadState()

                    val title = playwrightPage.title()
                    val content = extractContent(playwrightPage)

                    pages.add(ScrapedPage(url = url, title = title, content = content))

                    Thread.sleep(metadata.delayBetweenRequestsMs)

                    val progress = ((index + 1).toFloat() / urlsToScrape.size * 60 + 10).toInt()
                    updateProgress(job, progress)

                }.onFailure { e ->
                    log.warn { "Failed to scrape $url: ${e.message}" }
                }
            }
        } finally {
            playwrightPage.close()
        }

        return pages
    }

    private fun scrapePage(url: String, depth: Int): ScrapedPage {
        val page = getOrCreateBrowser().newPage()
        try {
            page.navigate(url)
            page.waitForLoadState()
            val title = page.title()
            val content = extractContent(page)
            return ScrapedPage(url = url, title = title, content = content, depth = depth)
        } finally {
            page.close()
        }
    }

    private fun extractLinks(
        page: Page,
        currentUrl: String,
        baseHost: String,
        metadata: WebScrapeJobMetadata,
        visited: Set<String>
    ): List<String> {
        return page.locator("a[href]").all()
            .mapNotNull { link -> link.getAttribute("href") }
            .filter { it.isNotBlank() }
            .map { resolveUrl(currentUrl, it) }
            .filter { url ->
                url !in visited &&
                    (metadata.followExternalLinks || runCatching { URI.create(url).host }.getOrNull() == baseHost)
            }
            .distinct()
    }

    private fun shouldProcessUrl(url: String, metadata: WebScrapeJobMetadata): Boolean {
        val includeMatches = metadata.includePatterns.isEmpty() ||
            metadata.includePatterns.any { pattern ->
                Pattern.compile(pattern).matcher(url).find()
            }

        val excludeMatches = metadata.excludePatterns.any { pattern ->
            Pattern.compile(pattern).matcher(url).find()
        }

        return includeMatches && !excludeMatches
    }

    private fun processScrapedPages(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        pages: List<ScrapedPage>
    ) {
        pages.forEach { page ->
            val knowledgeSourceId = createKnowledgeSource(job, knowledgeBaseId, page)
            val chunks = chunkContent(page.content)

            createEmbeddingJob(job, chunks, knowledgeSourceId, page)

            log.debug { "Created embedding job for ${page.url} with ${chunks.size} chunks" }
        }

        updateProgress(job, 90)
    }

    private fun createKnowledgeSource(
        job: IngestionJob,
        knowledgeBaseId: UUID,
        page: ScrapedPage
    ): UUID {
        val request = CreateKnowledgeSourceRequest(
            sourceType = SourceType.URL,
            fileName = null,
            sourceUrl = page.url,
            title = page.title.takeIf { it.isNotBlank() } ?: page.url,
            mimeType = "text/html",
            fileSize = page.content.length.toLong(),
            s3Key = null,
            ingestionJobId = job.id,
            metadata = mapOf(
                "depth" to page.depth,
                "parentJobId" to job.id.toString()
            )
        )

        return knowledgeSourceGateway.create(knowledgeBaseId, request).id
    }

    private fun createEmbeddingJob(
        parentJob: IngestionJob,
        chunks: List<String>,
        knowledgeSourceId: UUID,
        page: ScrapedPage
    ) {
        val chunkData = ChunkJobData(
            chunks = chunks.mapIndexed { index, content -> ChunkInfo(index, content) },
            sourceType = "URL",
            fileName = null,
            sourceUrl = page.url,
            title = page.title.takeIf { it.isNotBlank() } ?: page.url
        )

        val embeddingJob = IngestionJob(
            organizationId = parentJob.organizationId,
            jobType = JobType.EMBEDDING,
            knowledgeBaseId = parentJob.knowledgeBaseId,
            priority = parentJob.priority
        ).apply {
            parentJobId = parentJob.id
            this.knowledgeSourceId = knowledgeSourceId
            metadata = objectMapper.writeValueAsString(chunkData)
            sourceReference = page.url
        }

        val savedJob = jobRepository.save(embeddingJob)
        jobQueue.enqueue(savedJob)
    }

    private fun extractContent(page: Page): String {
        page.evaluate("""
            () => {
                const elementsToRemove = document.querySelectorAll('script, style, nav, header, footer, aside, .advertisement, .ads, [role="banner"], [role="navigation"]');
                elementsToRemove.forEach(el => el.remove());
            }
        """)

        val title = page.title()
        val body = page.locator("body").innerText()

        return buildString {
            appendLine("# $title")
            appendLine()
            appendLine(body)
        }
    }

    private fun resolveUrl(baseUrl: String, href: String): String {
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

    private fun chunkContent(content: String): List<String> {
        val chunkSize = ingestionProperties.processing.chunkSize
        val overlap = ingestionProperties.processing.chunkOverlap

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

    private fun updateProgress(job: IngestionJob, progress: Int) {
        job.updateProgress(progress)
        jobRepository.save(job)
    }
}
