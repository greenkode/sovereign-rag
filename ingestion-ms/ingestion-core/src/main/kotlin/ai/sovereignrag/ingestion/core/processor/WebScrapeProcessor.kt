package ai.sovereignrag.ingestion.core.processor

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.net.URI
import jakarta.annotation.PreDestroy

private val log = KotlinLogging.logger {}

@Component
class WebScrapeProcessor(
    private val jobRepository: IngestionJobRepository,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper
) {
    private val playwright: Playwright = Playwright.create()
    private val browser: Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions().setHeadless(true)
    )

    @PreDestroy
    fun cleanup() {
        browser.close()
        playwright.close()
    }

    fun process(job: IngestionJob) {
        log.info { "Processing web scrape job ${job.id}: ${job.sourceReference}" }

        val url = job.sourceReference
            ?: throw IllegalStateException("No URL for web scrape job ${job.id}")

        val metadata = job.metadata?.let {
            objectMapper.readValue(it, Map::class.java) as Map<String, Any>
        } ?: emptyMap()

        val crawl = metadata["crawl"] as? Boolean ?: false
        val maxDepth = (metadata["maxDepth"] as? Number)?.toInt() ?: 1
        val maxPages = (metadata["maxPages"] as? Number)?.toInt() ?: 10

        updateProgress(job, 10)

        val scrapedContent = if (crawl) {
            crawlAndScrape(url, maxDepth, maxPages, job)
        } else {
            listOf(scrapeUrl(url))
        }

        updateProgress(job, 80)

        val totalContent = scrapedContent.joinToString("\n\n---\n\n")
        val chunks = chunkContent(totalContent)

        log.info { "Scraped ${scrapedContent.size} pages, ${chunks.size} chunks from $url" }

        job.markCompleted(
            chunksCreated = chunks.size,
            bytesProcessed = totalContent.length.toLong()
        )
        jobRepository.save(job)
    }

    private fun scrapeUrl(url: String): String {
        val page = browser.newPage()
        try {
            page.navigate(url)
            page.waitForLoadState()
            return extractContent(page)
        } finally {
            page.close()
        }
    }

    private fun crawlAndScrape(
        startUrl: String,
        maxDepth: Int,
        maxPages: Int,
        job: IngestionJob
    ): List<String> {
        val visited = mutableSetOf<String>()
        val toVisit = mutableListOf(Pair(startUrl, 0))
        val content = mutableListOf<String>()
        val baseHost = URI.create(startUrl).host

        val page = browser.newPage()
        try {
            while (toVisit.isNotEmpty() && content.size < maxPages) {
                val (url, depth) = toVisit.removeFirst()

                if (url in visited) continue
                visited.add(url)

                try {
                    page.navigate(url)
                    page.waitForLoadState()

                    content.add(extractContent(page))

                    if (depth < maxDepth) {
                        val links = page.locator("a[href]").all()
                        links.forEach { link ->
                            val href = link.getAttribute("href")
                            if (!href.isNullOrBlank()) {
                                val absoluteUrl = resolveUrl(url, href)
                                if (absoluteUrl !in visited) {
                                    val linkHost = runCatching { URI.create(absoluteUrl).host }.getOrNull()
                                    if (linkHost == baseHost) {
                                        toVisit.add(Pair(absoluteUrl, depth + 1))
                                    }
                                }
                            }
                        }
                    }

                    Thread.sleep(ingestionProperties.scraping.delayBetweenRequestsMs)

                    val progress = ((content.size.toFloat() / maxPages) * 70 + 10).toInt()
                    updateProgress(job, progress)

                } catch (e: Exception) {
                    log.warn { "Failed to scrape $url: ${e.message}" }
                }
            }
        } finally {
            page.close()
        }

        return content
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
