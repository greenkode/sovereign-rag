package ai.sovereignrag.identity.core.config

import ai.sovereignrag.identity.core.service.AccountLockoutService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UserAuthenticationEventListener(
    private val accountLockoutService: AccountLockoutService
) {

    @EventListener
    fun onAuthenticationSuccess(event: AuthenticationSuccessEvent) {
        val authentication = event.authentication
        val username = extractUsername(authentication)
        
        if (username != null) {
            log.info { "User authentication success: $username" }
            accountLockoutService.handleSuccessfulLogin(username)
        }
    }

    @EventListener
    fun onAuthenticationFailure(event: AbstractAuthenticationFailureEvent) {
        val authentication = event.authentication
        val username = extractUsername(authentication)
        
        if (username != null) {
            log.warn { "User authentication failure: $username - ${event.exception.message}" }
            accountLockoutService.handleFailedLogin(username)
        }
    }
    
    private fun extractUsername(authentication: Any?): String? {
        return when (authentication) {
            is UsernamePasswordAuthenticationToken -> authentication.principal as? String ?: authentication.name
            is JwtAuthenticationToken -> authentication.name
            else -> null
        }
    }
}