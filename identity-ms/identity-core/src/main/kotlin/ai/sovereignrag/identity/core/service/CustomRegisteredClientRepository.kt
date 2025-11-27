package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthTokenSettingName
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
class CustomRegisteredClientRepository(
    private val clientRepository: OAuthRegisteredClientRepository
) : RegisteredClientRepository {

    override fun save(registeredClient: RegisteredClient) {
        log.debug { "Saving registered client: ${registeredClient.clientId}" }
        throw UnsupportedOperationException("Client registration not supported via this interface")
    }

    override fun findById(id: String): RegisteredClient? {
        log.debug { "Finding registered client by id: $id" }
        val client = clientRepository.findById(id).orElse(null)
        return client?.let { mapToRegisteredClient(it) }
    }

    override fun findByClientId(clientId: String): RegisteredClient? {
        log.info { "Finding registered client by clientId: $clientId" }
        val client = clientRepository.findByClientId(clientId)

        client?.takeIf { it.checkAndUnlockIfExpired() }?.let {
            clientRepository.save(it)
            log.info { "Lockout expired for client: ${it.clientId}, client unlocked" }
        }

        return client?.let { mapToRegisteredClient(it) }
    }

    private fun mapToRegisteredClient(entity: OAuthRegisteredClient): RegisteredClient {
        val builder = RegisteredClient.withId(entity.id)
            .clientId(entity.clientId)
            .clientIdIssuedAt(entity.clientIdIssuedAt)
            .clientName(entity.clientName)

        entity.clientSecret?.let { builder.clientSecret(it) }
        entity.clientSecretExpiresAt?.let { builder.clientSecretExpiresAt(it) }

        entity.authenticationMethods.forEach { authMethod ->
            builder.clientAuthenticationMethod(ClientAuthenticationMethod(authMethod.name.trim()))
        }

        entity.grantTypes.forEach { grantType ->
            builder.authorizationGrantType(AuthorizationGrantType(grantType.name.trim()))
        }

        entity.redirectUris.forEach { redirectUri ->
            builder.redirectUri(redirectUri.uri.trim())
        }

        entity.postLogoutRedirectUris.forEach { postLogoutUri ->
            builder.postLogoutRedirectUri(postLogoutUri.uri.trim())
        }

        entity.scopes.forEach { scope ->
            builder.scope(scope.name.trim())
        }

        val clientSettings = ClientSettings.builder().apply {
            entity.getSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT)?.let {
                requireAuthorizationConsent(it.toBoolean())
            }
            entity.getSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY)?.let {
                requireProofKey(it.toBoolean())
            }
        }.build()
        builder.clientSettings(clientSettings)

        val tokenSettings = TokenSettings.builder().apply {
            entity.getTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE)?.let {
                runCatching { accessTokenTimeToLive(Duration.parse(it)) }
                    .onFailure { e ->
                        log.warn { "Failed to parse accessTokenTimeToLive: $it, using default" }
                        accessTokenTimeToLive(Duration.ofMinutes(5))
                    }
            }
            entity.getTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE)?.let {
                runCatching { refreshTokenTimeToLive(Duration.parse(it)) }
                    .onFailure { e ->
                        log.warn { "Failed to parse refreshTokenTimeToLive: $it, using default" }
                        refreshTokenTimeToLive(Duration.ofDays(7))
                    }
            }
            entity.getTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS)?.let {
                runCatching { reuseRefreshTokens(it.toBoolean()) }
                    .onFailure { e ->
                        log.warn { "Failed to parse reuseRefreshTokens: $it, using default" }
                        reuseRefreshTokens(false)
                    }
            }
        }.build()
        builder.tokenSettings(tokenSettings)

        return builder.build()
    }
}
