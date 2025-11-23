package ai.sovereignrag.identity.core.service
import mu.KotlinLogging.logger
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class JwtBearerTokenInterceptor : ClientHttpRequestInterceptor {

    private val log = logger {}
    
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val jwtToken = getJwtToken()
        if (jwtToken != null) {
            request.headers.setBearerAuth(jwtToken)
            log.debug { "Added JWT Bearer token to request: ${request.uri}" }
        } else {
            log.warn { "No JWT token available for request: ${request.uri}" }
        }
        
        return execution.execute(request, body)
    }
    
    private fun getJwtToken(): String? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication is JwtAuthenticationToken) {
                authentication.token.tokenValue
            } else {
                log.debug { "No JWT token found in SecurityContext, authentication type: ${authentication?.javaClass?.simpleName}" }
                null
            }
        } catch (e: Exception) {
            log.error(e) { "Error extracting JWT token from SecurityContext" }
            null
        }
    }
}
