package ai.sovereignrag.identity.core.config


import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthTokenSettingName
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.ClientLockedException
import ai.sovereignrag.identity.core.service.ClientLockoutService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class ClientLockoutAuthenticationProvider(
    private val clientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clientLockoutService: ClientLockoutService
) : AuthenticationProvider {

    @Transactional
    override fun authenticate(authentication: Authentication): Authentication? {
        val clientAuth = authentication as? OAuth2ClientAuthenticationToken ?: return null

        val clientId = extractClientId(clientAuth) ?: return null
        val clientSecret = clientAuth.credentials as? String

        log.info { "Authenticating client: $clientId" }

        return runCatching {
            val clientEntity = clientRepository.findByClientId(clientId)
                ?: run {
                    log.warn { "Client not found: $clientId" }
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }

            clientEntity.takeIf { it.checkAndUnlockIfExpired() }?.let {
                clientRepository.save(it)
                log.info { "Lockout expired for client: $clientId, client unlocked and counter reset" }
            }

            clientEntity.takeIf { it.isCurrentlyLocked() }?.let {
                val now = Instant.now()
                val remainingSeconds = it.lockedUntil?.takeIf { lockedUntil -> now.isBefore(lockedUntil) }
                    ?.let { lockedUntil -> lockedUntil.epochSecond - now.epochSecond }
                    ?: 0L
                remainingSeconds / 60

                log.warn { "Client $clientId is locked until ${it.lockedUntil}" }
                throw ClientLockedException(
                    clientId = clientId,
                    lockedUntil = it.lockedUntil!!,
                    failedAttempts = it.failedAuthAttempts
                )
            }

            if (clientAuth.clientAuthenticationMethod == ClientAuthenticationMethod.CLIENT_SECRET_BASIC ||
                clientAuth.clientAuthenticationMethod == ClientAuthenticationMethod.CLIENT_SECRET_POST) {

                clientSecret ?: run {
                    log.warn { "Missing client secret for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }

                val secretsToCheck = listOfNotNull(
                    clientEntity.sandboxClientSecret,
                    clientEntity.productionClientSecret,
                    clientEntity.clientSecret
                )

                secretsToCheck.takeIf { it.isEmpty() }?.let {
                    log.warn { "No client secrets configured for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }

                val isValid = secretsToCheck.any { storedSecret ->
                    passwordEncoder.matches(clientSecret, storedSecret)
                }

                isValid.takeIf { !it }?.let {
                    log.warn { "Invalid client secret for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }
            }

            log.info { "Client authentication successful: $clientId" }
            clientLockoutService.handleSuccessfulClientAuth(clientId)

            val registeredClient = mapToRegisteredClient(clientEntity)
            OAuth2ClientAuthenticationToken(registeredClient, clientAuth.clientAuthenticationMethod, clientAuth.credentials)

        }.getOrElse { e ->
            when (e) {
                is ClientLockedException -> throw OAuth2AuthenticationException(
                    OAuth2Error(
                        "client_locked",
                        "Client is locked due to ${e.failedAttempts} failed authentication attempts. Try again in ${(e.lockedUntil.epochSecond - Instant.now().epochSecond) / 60} minutes.",
                        null
                    )
                )
                is OAuth2AuthenticationException -> throw e
                else -> {
                    log.error(e) { "Unexpected error during client authentication" }
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "An unexpected error occurred", null))
                }
            }
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return OAuth2ClientAuthenticationToken::class.java.isAssignableFrom(authentication)
    }

    private fun extractClientId(clientAuth: OAuth2ClientAuthenticationToken): String? {
        val principal = clientAuth.principal
        return when (principal) {
            is String -> principal
            is RegisteredClient -> principal.clientId
            else -> null
        }
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
