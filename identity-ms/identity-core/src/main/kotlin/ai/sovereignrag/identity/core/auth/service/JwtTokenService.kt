package ai.sovereignrag.identity.core.auth.service

import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.service.CustomUserDetails
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class JwtTokenService(
    private val tokenGenerator: OAuth2TokenGenerator<*>,
    private val authorizationServerSettings: AuthorizationServerSettings,
    @Value("\${identity-ms.token.expiry:600}") private val tokenExpiry: Long,
    @Value("\${identity-ms.refresh-token.expiry:86400}") private val refreshTokenExpiry: Long
) {

    fun generateToken(user: OAuthUser, userDetails: CustomUserDetails): String {
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val registeredClient = RegisteredClient.withId("direct-login-client")
            .clientId("akupay-payment-gateway")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiry))
                    .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiry))
                    .build()
            )
            .build()

        val settings = authorizationServerSettings
        val authServerContext = AuthorizationServerContextHolder.getContext()
            ?: object : AuthorizationServerContext {
                override fun getAuthorizationServerSettings() = settings
                override fun getIssuer() = settings.issuer
            }

        val tokenContext = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(authServerContext)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .build()

        val generatedToken = tokenGenerator.generate(tokenContext)
            ?: throw IllegalStateException("Failed to generate access token")

        return generatedToken.tokenValue.toString()
    }

    fun generateRefreshToken(user: OAuthUser): String {
        val userDetails = CustomUserDetails(user)
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val registeredClient = RegisteredClient.withId("direct-login-client")
            .clientId("akupay-payment-gateway")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiry))
                    .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiry))
                    .build()
            )
            .build()

        val settings = authorizationServerSettings
        val authServerContext = AuthorizationServerContextHolder.getContext()
            ?: object : AuthorizationServerContext {
                override fun getAuthorizationServerSettings() = settings
                override fun getIssuer() = settings.issuer
            }

        val tokenContext = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(authServerContext)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .tokenType(OAuth2TokenType.REFRESH_TOKEN)
            .build()

        val generatedToken = tokenGenerator.generate(tokenContext)
            ?: throw IllegalStateException("Failed to generate refresh token")

        return generatedToken.tokenValue.toString()
    }

    fun getTokenExpirySeconds(): Long = tokenExpiry

    fun getRefreshTokenExpirySeconds(): Long = refreshTokenExpiry
}
