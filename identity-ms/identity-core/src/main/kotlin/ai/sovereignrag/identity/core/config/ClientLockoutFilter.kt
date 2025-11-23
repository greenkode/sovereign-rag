package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ClientLockoutFilter(
    private val clientRepository: OAuthRegisteredClientRepository,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Only process OAuth2 token endpoint
        if (request.requestURI == "/oauth2/token") {
            val clientId = extractClientId(request)
            
            if (clientId != null) {
                val client = clientRepository.findByClientId(clientId)
                
                if (client != null) {
                    // Check and unlock if lockout has expired
                    if (client.checkAndUnlockIfExpired()) {
                        clientRepository.save(client)
                        log.info { "Lockout expired for client: ${client.clientId}, client unlocked" }
                    }
                    
                    // If client is still locked, block the request
                    if (client.isCurrentlyLocked()) {
                        log.warn { "Blocking locked client: $clientId" }
                        
                        val now = Instant.now()
                        val remainingSeconds = if (client.lockedUntil != null && now.isBefore(client.lockedUntil)) {
                            client.lockedUntil!!.epochSecond - now.epochSecond
                        } else {
                            0L
                        }
                        val remainingMinutes = remainingSeconds / 60
                        
                        val errorResponse = mapOf(
                            "error" to "client_locked",
                            "error_description" to "Client is locked due to ${client.failedAuthAttempts} failed authentication attempts. Try again in $remainingMinutes minutes.",
                            "failed_attempts" to client.failedAuthAttempts,
                            "locked_until" to (client.lockedUntil?.toString() ?: ""),
                            "remaining_minutes" to remainingMinutes
                        )
                        
                        response.status = HttpStatus.LOCKED.value()
                        response.contentType = MediaType.APPLICATION_JSON_VALUE
                        response.writer.write(objectMapper.writeValueAsString(errorResponse))
                        return
                    }
                }
            }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractClientId(request: HttpServletRequest): String? {
        // Try to get client_id from various sources
        
        // 1. From request parameter (client_secret_post)
        val clientIdParam = request.getParameter("client_id")
        if (clientIdParam != null) return clientIdParam
        
        // 2. From Basic Auth header (client_secret_basic)
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                val credentials = String(java.util.Base64.getDecoder().decode(authHeader.substring(6)))
                val parts = credentials.split(":")
                if (parts.isNotEmpty()) {
                    return parts[0]
                }
            } catch (e: Exception) {
                log.debug { "Failed to parse Basic auth header: ${e.message}" }
            }
        }
        
        return null
    }
}