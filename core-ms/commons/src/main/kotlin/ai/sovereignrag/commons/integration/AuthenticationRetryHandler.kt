package ai.sovereignrag.commons.integration

import ai.sovereignrag.commons.user.AccessTokenGateway
import ai.sovereignrag.commons.user.dto.AccessTokenDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * A handler that can be used with RestClient to automatically retry requests
 * when receiving 401 Unauthorized responses by refreshing the authentication token.
 * 
 * This handler prevents infinite retry loops by tracking retry attempts per integration
 * and limiting retries to once per minute.
 * 
 * Usage:
 * ```
 * restClient.get()
 *     .uri("/some/endpoint")
 *     .retrieve()
 *     .onStatus({ it == HttpStatus.UNAUTHORIZED }) { request, response ->
 *         authRetryHandler.handleUnauthorized(request) {
 *             // Retry the request with fresh token
 *             restClient.get()
 *                 .uri("/some/endpoint")
 *                 .retrieve()
 *                 .body(MyResponse::class.java)
 *         }
 *     }
 *     .body(MyResponse::class.java)
 * ```
 */
abstract class AuthenticationRetryHandler(
    protected val accessTokenGateway: AccessTokenGateway,
    protected val integrationCode: String,
    protected val integrationResource: String
) {
    private val log = KotlinLogging.logger {}
    
    companion object {
        // Global tracking of retry attempts per integration to prevent loops
        private val retryAttempts = ConcurrentHashMap<String, AtomicLong>()
        private val lastRetryTimestamp = ConcurrentHashMap<String, Long>()
        private const val MAX_RETRY_ATTEMPTS = 1
        private const val RETRY_RESET_INTERVAL_MS = 60000L // 1 minute
    }

    /**
     * Handles a 401 Unauthorized response by refreshing the token and retrying the request
     * 
     * @param retryAction A lambda that performs the retry request with the fresh token
     * @return The result of the retry action, or throws the original exception if retry fails
     */
    fun <T> handleUnauthorized(retryAction: () -> T): T {
        if (!canRetry()) {
            log.warn { "Cannot retry 401 for $integrationCode - retry limit exceeded" }
            throw org.springframework.web.client.HttpClientErrorException.Unauthorized.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null
            )
        }

        log.warn { "Received 401 Unauthorized for $integrationCode, attempting token refresh and retry" }
        incrementRetryCount()
        
        return try {
            // Clear existing token from cache
            clearTokenFromCache()
            
            // Get a fresh token
            refreshAuthToken()
            log.info { "Successfully refreshed token for $integrationCode" }
            
            // Retry the request with fresh token
            val result = retryAction()
            
            // Reset retry count on success
            resetRetryCount()
            
            result
        } catch (e: Exception) {
            log.error(e) { "Failed to retry request after token refresh for $integrationCode" }
            throw e
        }
    }

    private fun canRetry(): Boolean {
        val key = getRetryKey()
        val currentTime = System.currentTimeMillis()
        val lastRetry = lastRetryTimestamp[key] ?: 0L
        val attempts = retryAttempts[key]?.get() ?: 0L
        
        // Reset counter if enough time has passed
        if (currentTime - lastRetry > RETRY_RESET_INTERVAL_MS) {
            resetRetryCount()
            return true
        }
        
        return attempts < MAX_RETRY_ATTEMPTS
    }

    private fun incrementRetryCount() {
        val key = getRetryKey()
        retryAttempts.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
        lastRetryTimestamp[key] = System.currentTimeMillis()
        
        log.info { "Incremented retry count for $key to ${retryAttempts[key]?.get()}" }
    }

    private fun resetRetryCount() {
        val key = getRetryKey()
        retryAttempts[key]?.set(0)
        
        log.info { "Reset retry count for $key" }
    }

    private fun getRetryKey(): String = "${integrationCode}_${integrationResource}"

    private fun clearTokenFromCache() {
        try {
            val existingToken = accessTokenGateway.findLatestNonExpiredByIntegratorAndResource(
                integrationCode,
                integrationResource
            )
            
            if (existingToken != null) {
                log.info { "Clearing cached token for $integrationCode" }
                // The AccessTokenService should handle cache eviction when we save a new token
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to clear token from cache for $integrationCode" }
        }
    }

    /**
     * Abstract method that implementations must provide to fetch a new authentication token
     */
    abstract fun refreshAuthToken(): AccessTokenDto
}