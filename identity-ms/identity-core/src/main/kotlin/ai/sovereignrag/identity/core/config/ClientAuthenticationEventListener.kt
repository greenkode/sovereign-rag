package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.service.ClientLockoutService
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ClientAuthenticationEventListener(
    private val clientLockoutService: ClientLockoutService
) {

    @EventListener
    fun onAuthenticationSuccess(event: AuthenticationSuccessEvent) {
        val authentication = event.authentication
        
        if (authentication is OAuth2ClientAuthenticationToken) {
            val clientId = extractClientId(authentication)
            if (clientId != null) {
                log.info { "Client authentication success: $clientId" }
                clientLockoutService.handleSuccessfulClientAuth(clientId)
            }
        }
    }

    @EventListener
    fun onAuthenticationFailure(event: AbstractAuthenticationFailureEvent) {
        val authentication = event.authentication

        if (authentication is OAuth2ClientAuthenticationToken) {
            val clientId = extractClientId(authentication)
            if (clientId != null) {
                log.warn { "Client authentication failure: $clientId - ${event.exception.message}" }
            }
        }
    }
    
    private fun extractClientId(authentication: OAuth2ClientAuthenticationToken): String? {
        // Try to get from principal first
        val principal = authentication.principal
        return when (principal) {
            is String -> principal
            is org.springframework.security.oauth2.server.authorization.client.RegisteredClient -> principal.clientId
            else -> null
        }
    }
}