package ai.sovereignrag.ingestion.core.scraping

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SitemapParserTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sitemapParser: SitemapParser

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        sitemapParser = SitemapParser()
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `parse should extract URLs from simple urlset sitemap`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://example.com/page1</loc>
                    <lastmod>2024-01-15T10:30:00Z</lastmod>
                    <changefreq>weekly</changefreq>
                    <priority>0.8</priority>
                </url>
                <url>
                    <loc>https://example.com/page2</loc>
                    <priority>0.5</priority>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertEquals(2, result.size)
        assertEquals("https://example.com/page1", result[0].loc)
        assertEquals("https://example.com/page2", result[1].loc)
        assertEquals(0.8, result[0].priority)
        assertEquals(0.5, result[1].priority)
        assertEquals("weekly", result[0].changefreq)
        assertNotNull(result[0].lastmod)
    }

    @Test
    fun `parse should filter URLs below priority threshold`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://example.com/high-priority</loc>
                    <priority>0.9</priority>
                </url>
                <url>
                    <loc>https://example.com/low-priority</loc>
                    <priority>0.3</priority>
                </url>
                <url>
                    <loc>https://example.com/no-priority</loc>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString(), priorityThreshold = 0.5)

        assertEquals(2, result.size)
        assertEquals("https://example.com/high-priority", result[0].loc)
        assertEquals("https://example.com/no-priority", result[1].loc)
    }

    @Test
    fun `parse should handle sitemapindex and follow nested sitemaps`() {
        val indexXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <sitemap>
                    <loc>${mockWebServer.url("/sitemap1.xml")}</loc>
                </sitemap>
            </sitemapindex>
        """.trimIndent()

        val nestedSitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://example.com/nested-page</loc>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(indexXml).setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody(nestedSitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertEquals(1, result.size)
        assertEquals("https://example.com/nested-page", result[0].loc)
    }

    @Test
    fun `parse should handle date-only lastmod format`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://example.com/page</loc>
                    <lastmod>2024-06-15</lastmod>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertEquals(1, result.size)
        assertNotNull(result[0].lastmod)
        assertTrue(result[0].lastmod!!.isBefore(Instant.now()))
    }

    @Test
    fun `parse should skip URLs with missing loc`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <priority>0.8</priority>
                </url>
                <url>
                    <loc>https://example.com/valid</loc>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertEquals(1, result.size)
        assertEquals("https://example.com/valid", result[0].loc)
    }

    @Test
    fun `parse should throw SitemapParseException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        assertThrows<SitemapParseException> {
            sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())
        }
    }

    @Test
    fun `parse should handle empty urlset`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse should handle URLs without optional fields`() {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://example.com/minimal</loc>
                </url>
            </urlset>
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(sitemapXml).setResponseCode(200))

        val result = sitemapParser.parse(mockWebServer.url("/sitemap.xml").toString())

        assertEquals(1, result.size)
        assertEquals("https://example.com/minimal", result[0].loc)
        assertNull(result[0].lastmod)
        assertNull(result[0].changefreq)
        assertNull(result[0].priority)
    }
}
