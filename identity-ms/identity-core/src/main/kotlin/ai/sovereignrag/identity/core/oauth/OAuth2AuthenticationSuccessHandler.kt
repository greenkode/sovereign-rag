package ai.sovereignrag.identity.core.oauth

import ai.sovereignrag.identity.commons.audit.AuditPayloadKey
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.service.ClientIpExtractionService
import ai.sovereignrag.identity.core.service.CustomUserDetails
import ai.sovereignrag.identity.core.trusteddevice.service.DeviceFingerprintService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class OAuth2AuthenticationSuccessHandler(
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val clientIpExtractionService: ClientIpExtractionService,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Value("\${app.oauth.frontend-callback-url:http://localhost:3000/auth/oauth-callback}")
    private val frontendCallbackUrl: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as? OAuth2UserPrincipal
            ?: run {
                log.error { "OAuth2 authentication succeeded but principal is not OAuth2UserPrincipal" }
                response.sendRedirect("$frontendCallbackUrl?error=invalid_principal")
                return
            }

        val user = principal.internalUser
        val provider = principal.provider
        val ipAddress = clientIpExtractionService.getClientIpAddress(request)
        val userAgent = request.getHeader(HttpHeaders.USER_AGENT)
        val deviceFingerprint = deviceFingerprintService.generateFingerprintFromRequest(request, ipAddress)

        log.info { "OAuth2 login successful for user: ${user.email} via $provider" }

        val userDetails = CustomUserDetails(user)
        val accessToken = jwtTokenService.generateToken(user, userDetails)
        val refreshToken = refreshTokenService.createRefreshToken(
            user = user,
            ipAddress = ipAddress,
            userAgent = userAgent,
            deviceFingerprint = deviceFingerprint
        )

        publishAuditEvent(user.id.toString(), user.email, provider.name, ipAddress)

        val redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
            .queryParam("access_token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
            .queryParam("refresh_token", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
            .queryParam("expires_in", jwtTokenService.getTokenExpirySeconds())
            .queryParam("provider", provider.name.lowercase())
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }

    private fun publishAuditEvent(userId: String, email: String, provider: String, ipAddress: String?) {
        applicationEventPublisher.publishEvent(
            ai.sovereignrag.identity.commons.audit.AuditEvent(
                actorId = userId,
                actorName = email,
                merchantId = "unknown",
                identityType = ai.sovereignrag.identity.commons.audit.IdentityType.USER,
                resource = ai.sovereignrag.identity.commons.audit.AuditResource.IDENTITY,
                event = "OAuth2 login successful via $provider",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    AuditPayloadKey.USER_ID.value to userId,
                    AuditPayloadKey.USERNAME.value to email,
                    AuditPayloadKey.LOGIN_METHOD.value to "OAUTH2_$provider",
                    AuditPayloadKey.IP_ADDRESS.value to (ipAddress ?: "unknown")
                )
            )
        )
    }
}
