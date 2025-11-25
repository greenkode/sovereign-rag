package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
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
        (request.requestURI == "/oauth2/token")
            .takeIf { it }
            ?.let { extractClientId(request) }
            ?.let { clientId -> clientRepository.findByClientId(clientId)?.also { processClientLockout(it, response) } }

        filterChain.doFilter(request, response)
    }

    private fun processClientLockout(client: ai.sovereignrag.identity.core.entity.OAuthRegisteredClient, response: HttpServletResponse) {
        client.checkAndUnlockIfExpired()
            .takeIf { it }
            ?.also {
                clientRepository.save(client)
                log.info { "Lockout expired for client: ${client.clientId}, client unlocked" }
            }

        client.takeIf { it.isCurrentlyLocked() }?.let {
            log.warn { "Blocking locked client: ${client.clientId}" }

            val now = Instant.now()
            val remainingMinutes = client.lockedUntil
                ?.takeIf { now.isBefore(it) }
                ?.let { (it.epochSecond - now.epochSecond) / 60 }
                ?: 0L

            val errorResponse = mapOf(
                "error" to "client_locked",
                "error_description" to "Client is locked due to ${client.failedAuthAttempts} failed authentication attempts. Try again in $remainingMinutes minutes.",
                "failed_attempts" to client.failedAuthAttempts,
                "locked_until" to client.lockedUntil?.toString().orEmpty(),
                "remaining_minutes" to remainingMinutes
            )

            response.status = HttpStatus.LOCKED.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
        }
    }
    
    private fun extractClientId(request: HttpServletRequest): String? =
        request.getParameter("client_id")
            ?: extractClientIdFromBasicAuth(request)

    private fun extractClientIdFromBasicAuth(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Basic ") }
            ?.let { header ->
                runCatching {
                    String(java.util.Base64.getDecoder().decode(header.substring(6)))
                        .split(":")
                        .firstOrNull()
                }.onFailure { e ->
                    log.debug { "Failed to parse Basic auth header: ${e.message}" }
                }.getOrNull()
            }
}