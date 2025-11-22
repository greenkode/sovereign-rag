package ai.sovereignrag.support.zoho.interceptor

import ai.sovereignrag.support.zoho.dto.ZohoAuthResponse
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient
import java.time.Instant

class ZohoAuthInterceptor(
    private val clientId: String,
    private val clientSecret: String,
    private val refreshToken: String,
    private val authUrl: String
) : ClientHttpRequestInterceptor {

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var tokenExpiryTime: Instant? = null

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        if (isTokenExpired()) {
            synchronized(this) {
                if (isTokenExpired()) {
                    refreshAccessToken()
                }
            }
        }

        accessToken?.let {
            request.headers.setBearerAuth(it)
        }

        return execution.execute(request, body)
    }

    private fun isTokenExpired(): Boolean {
        return accessToken == null || tokenExpiryTime == null ||
               Instant.now().isAfter(tokenExpiryTime)
    }

    private fun refreshAccessToken() {
        val restClient = RestClient.create()

        val response = restClient.post()
            .uri(authUrl)
            .body(mapOf(
                "refresh_token" to refreshToken,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "grant_type" to "refresh_token"
            ))
            .retrieve()
            .body(ZohoAuthResponse::class.java)

        response?.let {
            accessToken = it.accessToken
            tokenExpiryTime = Instant.now().plusSeconds(it.expiresIn - 300) // Refresh 5 minutes early
        }
    }
}
