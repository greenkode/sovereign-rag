package ai.sovereignrag.identity.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CacheEvictionService(
    private val cacheManager: CacheManager
) {

    private fun evictCache(cacheName: String, key: String, keyType: String) {
        try {
            log.info { "Evicting $cacheName cache for $keyType: $key" }
            cacheManager.getCache(cacheName)?.evict(key)
            log.info { "Successfully evicted $cacheName cache for $keyType: $key" }
        } catch (e: Exception) {
            log.error(e) { "Failed to evict $cacheName cache for $keyType: $key" }
        }
    }

    fun evictUserCaches(userId: String) {
        evictCache("KycUser", userId, "userId")
    }

    fun evictMerchantCaches(merchantId: String) {
        evictCache("MerchantDetails", merchantId, "merchantId")
    }

    fun evictUserDetailsCaches(userId: String) {
        evictCache("UserDetails", userId, "userId")
    }
}