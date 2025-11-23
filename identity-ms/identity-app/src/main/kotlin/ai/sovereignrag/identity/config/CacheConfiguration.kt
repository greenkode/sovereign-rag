package ai.sovereignrag.identity.config

import ai.sovereignrag.identity.commons.cache.IdentityCache
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
        val identityNames = IdentityCache.entries.map { it.cacheName }
        val coreCacheNames = listOf("KycUser", "MerchantDetails", "UserDetails")
        val allNames = (identityNames + coreCacheNames).toTypedArray()
        return ConcurrentMapCacheManager(*allNames)
    }
}