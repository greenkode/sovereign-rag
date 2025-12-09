package ai.sovereignrag.ingestion.core.scraping

import ai.sovereignrag.ingestion.commons.model.SitemapEntry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

private val log = KotlinLogging.logger {}

@Component
class SitemapParser {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun parse(sitemapUrl: String, priorityThreshold: Double = 0.0): List<SitemapEntry> {
        log.info { "Parsing sitemap: $sitemapUrl" }

        val response = fetchSitemap(sitemapUrl)
        val entries = parseSitemapXml(response, sitemapUrl, priorityThreshold)

        log.info { "Found ${entries.size} URLs in sitemap" }
        return entries
    }

    private fun fetchSitemap(url: String): InputStream {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "SovereignRAG-Bot/1.0")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        response.statusCode().takeIf { it !in 200..299 }?.let {
            throw SitemapParseException("Failed to fetch sitemap: HTTP $it")
        }

        return response.body()
    }

    private fun parseSitemapXml(
        inputStream: InputStream,
        sitemapUrl: String,
        priorityThreshold: Double
    ): List<SitemapEntry> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)

        val rootElement = document.documentElement.localName

        return when (rootElement) {
            "sitemapindex" -> parseSitemapIndex(document.getElementsByTagNameNS("*", "sitemap"), priorityThreshold)
            "urlset" -> parseUrlset(document.getElementsByTagNameNS("*", "url"), priorityThreshold)
            else -> {
                log.warn { "Unknown sitemap root element: $rootElement in $sitemapUrl" }
                emptyList()
            }
        }
    }

    private fun parseSitemapIndex(sitemapNodes: NodeList, priorityThreshold: Double): List<SitemapEntry> {
        val allEntries = mutableListOf<SitemapEntry>()

        for (i in 0 until sitemapNodes.length) {
            val sitemapElement = sitemapNodes.item(i) as? Element ?: continue
            val loc = sitemapElement.getTextContent("loc")

            loc?.let { nestedUrl ->
                log.debug { "Processing nested sitemap: $nestedUrl" }
                runCatching {
                    allEntries.addAll(parse(nestedUrl, priorityThreshold))
                }.onFailure { e ->
                    log.warn { "Failed to parse nested sitemap $nestedUrl: ${e.message}" }
                }
            }
        }

        return allEntries
    }

    private fun parseUrlset(urlNodes: NodeList, priorityThreshold: Double): List<SitemapEntry> {
        val entries = mutableListOf<SitemapEntry>()

        for (i in 0 until urlNodes.length) {
            val urlElement = urlNodes.item(i) as? Element ?: continue

            val loc = urlElement.getTextContent("loc") ?: continue
            val lastmod = urlElement.getTextContent("lastmod")?.let { parseLastmod(it) }
            val changefreq = urlElement.getTextContent("changefreq")
            val priority = urlElement.getTextContent("priority")?.toDoubleOrNull()

            val entry = SitemapEntry(
                loc = loc,
                lastmod = lastmod,
                changefreq = changefreq,
                priority = priority
            )

            (priority ?: 0.5).takeIf { it >= priorityThreshold }?.let {
                entries.add(entry)
            }
        }

        return entries
    }

    private fun Element.getTextContent(tagName: String): String? {
        val nodes = getElementsByTagNameNS("*", tagName)
        return nodes.item(0)?.textContent?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseLastmod(lastmod: String): Instant? {
        return runCatching {
            when {
                lastmod.contains('T') -> Instant.parse(lastmod)
                else -> Instant.parse("${lastmod}T00:00:00Z")
            }
        }.getOrNull()
    }
}

class SitemapParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
