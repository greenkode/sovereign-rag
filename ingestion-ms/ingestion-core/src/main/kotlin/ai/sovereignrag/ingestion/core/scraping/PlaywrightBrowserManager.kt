package ai.sovereignrag.ingestion.core.scraping

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

@Component
class PlaywrightBrowserManager {

    private val lock = ReentrantLock()
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    fun getOrCreateBrowser(): Browser {
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

    fun <T> withPage(action: (Page) -> T): T {
        val page = getOrCreateBrowser().newPage()
        return try {
            action(page)
        } finally {
            page.close()
        }
    }

    fun extractContent(page: Page): String {
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
}
