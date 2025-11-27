package ai.sovereignrag.identity.config

import ai.sovereignrag.commons.cache.CacheNames
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "simple", matchIfMissing = true)
class IdentitySimpleCacheConfig {
    @Bean
    fun identityCacheManager(): CacheManager {
        val identitySpecificCaches = listOf(
            CacheNames.USER_SETTINGS,
            CacheNames.MERCHANT_CLIENT,
            CacheNames.OAUTH_USER,
            CacheNames.USER_ROLES,
            CacheNames.REGISTERED_CLIENT,
            CacheNames.KYC_USER,
            CacheNames.MERCHANT_DETAILS,
            CacheNames.USER_DETAILS,
            CacheNames.PROCESS
        )
        return ConcurrentMapCacheManager(*identitySpecificCaches.toTypedArray())
    }
}