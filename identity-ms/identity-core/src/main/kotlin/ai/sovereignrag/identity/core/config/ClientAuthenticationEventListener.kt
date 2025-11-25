package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.service.ClientLockoutService
import io.github.oshai.kotlinlogging.KotlinLogging
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
        (event.authentication as? OAuth2ClientAuthenticationToken)
            ?.let { extractClientId(it) }
            ?.let { clientId ->
                log.info { "Client authentication success: $clientId" }
                clientLockoutService.handleSuccessfulClientAuth(clientId)
            }
    }

    @EventListener
    fun onAuthenticationFailure(event: AbstractAuthenticationFailureEvent) {
        (event.authentication as? OAuth2ClientAuthenticationToken)
            ?.let { extractClientId(it) }
            ?.let { clientId ->
                log.warn { "Client authentication failure: $clientId - ${event.exception.message}" }
            }
    }

    private fun extractClientId(authentication: OAuth2ClientAuthenticationToken): String? =
        when (val principal = authentication.principal) {
            is String -> principal
            is org.springframework.security.oauth2.server.authorization.client.RegisteredClient -> principal.clientId
            else -> null
        }
}
