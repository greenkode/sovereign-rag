package ai.sovereignrag.identity.config

import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
@Order(1)
class IdentityCacheStartupInitializer(
    private val cacheManager: CacheManager
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments?) {
        log.info { "Identity service startup detected - initiating cache flush" }

        try {
            val cacheNames = cacheManager.cacheNames.toList()
            log.info { "Found ${cacheNames.size} identity caches to flush: ${cacheNames.joinToString(", ")}" }

            var clearedCount = 0
            val failedCaches = mutableListOf<String>()

            for (cacheName in cacheNames) {
                try {
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                    clearedCount++
                    log.debug { "Successfully flushed identity cache: $cacheName" }
                } catch (e: Exception) {
                    log.warn(e) { "Failed to flush identity cache on startup: $cacheName" }
                    failedCaches.add(cacheName)
                }
            }

            if (failedCaches.isEmpty()) {
                log.info { "Identity cache flush completed successfully - cleared $clearedCount caches" }
            } else {
                log.warn {
                    "Identity cache flush completed with errors - cleared $clearedCount caches. " +
                    "Failed to clear: ${failedCaches.joinToString(", ")}"
                }
            }

        } catch (e: Exception) {
            log.error(e) { "Critical error during identity cache flush on startup" }
        }
    }
}