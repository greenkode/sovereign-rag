package ai.sovereignrag.identity.commons.cache

import java.time.Duration
import java.util.concurrent.TimeUnit

enum class IdentityCache(val cacheName: String, val ttl: Long, val timeUnit: TimeUnit) {

    USER_SETTINGS(IdentityCacheNames.USER_SETTINGS, 15, TimeUnit.MINUTES),
    MERCHANT_CLIENT(IdentityCacheNames.MERCHANT_CLIENT, 30, TimeUnit.MINUTES),
    OAUTH_USER(IdentityCacheNames.OAUTH_USER, 10, TimeUnit.MINUTES),
    USER_ROLES(IdentityCacheNames.USER_ROLES, 20, TimeUnit.MINUTES);

    fun computeTtl(ttlTimeUnit: TimeUnit, ttl: Long): Duration {
        return when (ttlTimeUnit) {
            TimeUnit.SECONDS -> Duration.ofSeconds(ttl)
            TimeUnit.MINUTES -> Duration.ofMinutes(ttl)
            TimeUnit.HOURS -> Duration.ofHours(ttl)
            TimeUnit.DAYS -> Duration.ofDays(ttl)
            else -> Duration.ofSeconds(60)
        }
    }
}