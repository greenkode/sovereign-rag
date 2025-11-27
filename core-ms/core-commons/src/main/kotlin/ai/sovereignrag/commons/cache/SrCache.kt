package ai.sovereignrag.commons.cache

import java.time.Duration
import java.util.concurrent.TimeUnit

object CacheNames {
    const val ACCOUNT = "accounts"
    const val PRICING_CALC = "PricingCalc"
    const val USER_DETAILS = "UserDetails"
    const val ACCOUNT_BALANCE = "AccountBalance"
    const val POOL_ACCOUNT = "PoolAccount"
    const val SYSTEM_PROPERTIES = "SystemProps"
    const val AUTH_TOKEN = "AuthToken"
    const val KYC_USER = "KycUser"
    const val MERCHANT_DETAILS = "MerchantDetails"
    const val PROCESS = "Process"
    const val WEBHOOK_CONFIG = "WebhookConfig"
    const val INTEGRATION_RESPONSE_CODE = "IntegrationResponseCode"
    const val CHAT_SESSION = "ChatSession"
    const val CHAT_MESSAGES = "ChatMessages"
    const val CHAT_RESPONSES = "ChatResponses"
    const val AGENT_CONFIG = "AgentConfig"
    const val MINIGL_BALANCE_SNAPSHOTS = "minigl-balance-snapshots"
    const val MINIGL_DAILY_LIMITS = "minigl-daily-limits"
    const val MINIGL_TRANSACTIONS = "minigl-transactions"
    const val MINIGL_TRANSACTION_LIMITS = "minigl-transaction-limits"
    const val MINIGL_ACCOUNT_PROFILES = "minigl-account-profiles"
}

enum class SrCache(val cacheName: String, val ttl: Long, val timeUnit: TimeUnit) {

    ACCOUNT(CacheNames.ACCOUNT, 60, TimeUnit.MINUTES),
    
    // Tier 1: Critical Path Performance (Highest Impact)
    PRICING_CALC(CacheNames.PRICING_CALC, 10, TimeUnit.MINUTES),
    USER_DETAILS(CacheNames.USER_DETAILS, 15, TimeUnit.MINUTES),
    PROCESS(CacheNames.PROCESS, 5, TimeUnit.MINUTES),
    
    // Tier 2: High-Frequency Data  
    ACCOUNT_BALANCE(CacheNames.ACCOUNT_BALANCE, 5, TimeUnit.MINUTES),
    POOL_ACCOUNT(CacheNames.POOL_ACCOUNT, 2, TimeUnit.HOURS),
    
    // Tier 3: Configuration & Metadata
    SYSTEM_PROPERTIES(CacheNames.SYSTEM_PROPERTIES, 10, TimeUnit.MINUTES),
    AUTH_TOKEN(CacheNames.AUTH_TOKEN, 30, TimeUnit.MINUTES),
    KYC_USER(CacheNames.KYC_USER, 20, TimeUnit.MINUTES),
    MERCHANT_DETAILS(CacheNames.MERCHANT_DETAILS, 15, TimeUnit.MINUTES),
    WEBHOOK_CONFIG(CacheNames.WEBHOOK_CONFIG, 30, TimeUnit.MINUTES),
    INTEGRATION_RESPONSE_CODE(CacheNames.INTEGRATION_RESPONSE_CODE, 4, TimeUnit.HOURS),
    CHAT_SESSION(CacheNames.CHAT_SESSION, 30, TimeUnit.MINUTES),
    CHAT_MESSAGES(CacheNames.CHAT_MESSAGES, 30, TimeUnit.MINUTES),
    CHAT_RESPONSES(CacheNames.CHAT_RESPONSES, 30, TimeUnit.MINUTES),
    AGENT_CONFIG(CacheNames.AGENT_CONFIG, 24, TimeUnit.HOURS),

    // MiniGL specific caches
    MINIGL_BALANCE_SNAPSHOTS(CacheNames.MINIGL_BALANCE_SNAPSHOTS, 30, TimeUnit.MINUTES),
    MINIGL_DAILY_LIMITS(CacheNames.MINIGL_DAILY_LIMITS, 24, TimeUnit.HOURS),
    MINIGL_TRANSACTIONS(CacheNames.MINIGL_TRANSACTIONS, 30, TimeUnit.MINUTES),
    MINIGL_TRANSACTION_LIMITS(CacheNames.MINIGL_TRANSACTION_LIMITS, 60, TimeUnit.MINUTES),
    MINIGL_ACCOUNT_PROFILES(CacheNames.MINIGL_ACCOUNT_PROFILES, 120, TimeUnit.MINUTES);

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