package ai.sovereignrag.identity.core.oauth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.oauth.frontend-callback-url:http://localhost:3001/auth/oauth-callback}")
    private val frontendCallbackUrl: String
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        log.error(exception) { "OAuth2 authentication failed: ${exception.message}" }

        val errorMessage = exception.message ?: "Authentication failed"

        val redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
            .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }
}
