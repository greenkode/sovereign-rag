package ai.sovereignrag.commons.integration

import ai.sovereignrag.commons.user.AccessTokenGateway
import ai.sovereignrag.commons.user.dto.AccessTokenDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

abstract class AuthenticationRetryInterceptor(
    protected val accessTokenGateway: AccessTokenGateway,
    private val integrationCode: String,
    private val integrationResource: String
) : ClientHttpRequestInterceptor {

    private val log = KotlinLogging.logger {}
    
    companion object {
        // Global tracking of retry attempts per integration to prevent loops across instances
        private val retryAttempts = ConcurrentHashMap<String, AtomicLong>()
        private val lastRetryTimestamp = ConcurrentHashMap<String, Long>()
        private const val MAX_RETRY_ATTEMPTS = 1
        private const val RETRY_RESET_INTERVAL_MS = 60000L // 1 minute
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        
        val token = getOrRefreshToken()
        request.headers.setBearerAuth(token.accessToken)
        
        // Execute the request and buffer the response to allow multiple reads
        val response = execution.execute(request, body)
        val bufferedResponse = BufferedClientHttpResponse(response)
        
        // If we get a 401, try to refresh the token and retry once
        if (bufferedResponse.statusCode == HttpStatus.UNAUTHORIZED && canRetry()) {
            log.warn { "Received 401 Unauthorized for $integrationCode, attempting token refresh and retry" }
            
            // Increment retry counter
            incrementRetryCount()
            
            try {
                // Clear existing token from cache
                clearTokenFromCache()
                
                // Get a fresh token
                val freshToken = refreshAuthToken()
                
                // Create new request with fresh token
                request.headers.setBearerAuth(freshToken.accessToken)
                
                // Retry the request
                val retryResponse = execution.execute(request, body)
                
                if (retryResponse.statusCode == HttpStatus.UNAUTHORIZED) {
                    log.error { "Still getting 401 after token refresh for $integrationCode. Authentication may be fundamentally broken." }
                }
                
                // Reset retry count on successful token refresh
                resetRetryCount()
                
                return retryResponse
                
            } catch (e: Exception) {
                log.error(e) { "Failed to refresh token for $integrationCode during 401 recovery" }
                // Return buffered 401 response if refresh fails
                return bufferedResponse
            }
        }
        
        // Reset retry count on successful request (non-401)
        if (bufferedResponse.statusCode != HttpStatus.UNAUTHORIZED) {
            resetRetryCount()
        }
        
        return bufferedResponse
    }

    private fun getOrRefreshToken(): AccessTokenDto {
        log.info { "Getting token for $integrationCode/$integrationResource" }
        
        val existingToken = accessTokenGateway.findLatestNonExpiredByIntegratorAndResource(
            integrationCode,
            integrationResource
        )
        
        if (existingToken != null) {
            val remainingSeconds = existingToken.expiry.epochSecond - java.time.Instant.now().epochSecond
            log.info { 
                "Found existing token for $integrationCode with ${remainingSeconds}s remaining" 
            }
            return existingToken
        }
        
        log.info { "No valid token found for $integrationCode, refreshing..." }
        return refreshAuthToken()
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
            // Find and evict the token from cache
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
     * This method should make the actual authentication call to the external service
     */
    abstract fun refreshAuthToken(): AccessTokenDto

    /**
     * Buffered response that allows the response body to be read multiple times
     */
    private class BufferedClientHttpResponse(response: ClientHttpResponse) : ClientHttpResponse {
        private val bufferedBody: ByteArray = response.body.use { it.readAllBytes() }
        private val headers = response.headers
        private val status = response.statusCode
        private val statusText = response.statusText ?: ""
        
        override fun getBody(): InputStream = ByteArrayInputStream(bufferedBody)
        override fun getHeaders() = headers
        override fun getStatusCode() = status
        override fun getStatusText() = statusText
        override fun close() {
            // Nothing to close as we've already read the body
        }
    }
}