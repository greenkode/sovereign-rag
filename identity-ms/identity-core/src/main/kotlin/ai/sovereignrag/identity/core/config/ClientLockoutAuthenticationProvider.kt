package ai.sovereignrag.identity.core.config


import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.service.ClientLockedException
import ai.sovereignrag.identity.core.service.ClientLockoutService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class ClientLockoutAuthenticationProvider(
    private val clientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clientLockoutService: ClientLockoutService,
    private val objectMapper: ObjectMapper
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        val clientAuth = authentication as? OAuth2ClientAuthenticationToken ?: return null
        
        val clientId = extractClientId(clientAuth) ?: return null
        val clientSecret = clientAuth.credentials as? String
        
        log.info { "Authenticating client: $clientId" }
        
        try {
            // First check if client exists in our database
            val clientEntity = clientRepository.findByClientId(clientId)
            if (clientEntity == null) {
                log.warn { "Client not found: $clientId" }
                throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
            }

            // Check if lockout has expired and unlock if so
            if (clientEntity.checkAndUnlockIfExpired()) {
                clientRepository.save(clientEntity)
                log.info { "Lockout expired for client: $clientId, client unlocked and counter reset" }
            }

            // Check if client is locked
            if (clientEntity.isCurrentlyLocked()) {
                val now = Instant.now()
                val remainingSeconds = if (clientEntity.lockedUntil != null && now.isBefore(clientEntity.lockedUntil)) {
                    clientEntity.lockedUntil!!.epochSecond - now.epochSecond
                } else {
                    0L
                }
                remainingSeconds / 60
                
                log.warn { "Client $clientId is locked until ${clientEntity.lockedUntil}" }
                throw ClientLockedException(
                    clientId = clientId,
                    lockedUntil = clientEntity.lockedUntil!!,
                    failedAttempts = clientEntity.failedAuthAttempts
                )
            }
            
            // Verify client secret for client_secret_basic or client_secret_post
            if (clientAuth.clientAuthenticationMethod == ClientAuthenticationMethod.CLIENT_SECRET_BASIC ||
                clientAuth.clientAuthenticationMethod == ClientAuthenticationMethod.CLIENT_SECRET_POST) {

                if (clientSecret == null) {
                    log.warn { "Missing client secret for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }

                val secretsToCheck = listOfNotNull(
                    clientEntity.sandboxClientSecret,
                    clientEntity.productionClientSecret,
                    clientEntity.clientSecret
                )

                if (secretsToCheck.isEmpty()) {
                    log.warn { "No client secrets configured for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }

                val isValid = secretsToCheck.any { storedSecret ->
                    passwordEncoder.matches(clientSecret, storedSecret)
                }

                if (!isValid) {
                    log.warn { "Invalid client secret for client: $clientId" }
                    clientLockoutService.handleFailedClientAuth(clientId)
                    throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null))
                }
            }
            
            // Authentication successful - convert entity to RegisteredClient
            log.info { "Client authentication successful: $clientId" }
            clientLockoutService.handleSuccessfulClientAuth(clientId)
            
            val registeredClient = mapToRegisteredClient(clientEntity)
            return OAuth2ClientAuthenticationToken(registeredClient, clientAuth.clientAuthenticationMethod, clientAuth.credentials)
            
        } catch (e: ClientLockedException) {
            // Re-throw lockout exception with proper OAuth2 error
            throw OAuth2AuthenticationException(
                OAuth2Error(
                    "client_locked",
                    "Client is locked due to ${e.failedAttempts} failed authentication attempts. Try again in ${(e.lockedUntil.epochSecond - Instant.now().epochSecond) / 60} minutes.",
                    null
                )
            )
        } catch (e: OAuth2AuthenticationException) {
            // Re-throw OAuth2 exceptions
            throw e
        } catch (e: Exception) {
            log.error(e) { "Unexpected error during client authentication" }
            throw OAuth2AuthenticationException(OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "An unexpected error occurred", null))
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return OAuth2ClientAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
    
    private fun extractClientId(clientAuth: OAuth2ClientAuthenticationToken): String? {
        // Try to get from principal first
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
        
        entity.clientAuthenticationMethods.split(",").forEach { method ->
            builder.clientAuthenticationMethod(ClientAuthenticationMethod(method.trim()))
        }
        
        entity.authorizationGrantTypes.split(",").forEach { grantType ->
            builder.authorizationGrantType(AuthorizationGrantType(grantType.trim()))
        }
        
        entity.redirectUris?.split(",")?.forEach { uri ->
            builder.redirectUri(uri.trim())
        }
        
        entity.postLogoutRedirectUris?.split(",")?.forEach { uri ->
            builder.postLogoutRedirectUri(uri.trim())
        }
        
        entity.scopes.split(",").forEach { scope ->
            builder.scope(scope.trim())
        }
        
        val clientSettingsMap: Map<String, Any> = objectMapper.readValue(entity.clientSettings)
        val clientSettings = ClientSettings.builder().apply {
            clientSettingsMap["requireAuthorizationConsent"]?.let {
                requireAuthorizationConsent(it.toString().toBoolean())
            }
            clientSettingsMap["requireProofKey"]?.let {
                requireProofKey(it.toString().toBoolean())
            }
        }.build()
        builder.clientSettings(clientSettings)
        
        val tokenSettingsMap: Map<String, Any> = objectMapper.readValue(entity.tokenSettings)
        val tokenSettings = TokenSettings.builder().apply {
            tokenSettingsMap["accessTokenTimeToLive"]?.let {
                try {
                    accessTokenTimeToLive(Duration.parse(it as String))
                } catch (e: Exception) {
                    log.warn { "Failed to parse accessTokenTimeToLive: $it, using default" }
                    accessTokenTimeToLive(Duration.ofMinutes(5))
                }
            }
            tokenSettingsMap["refreshTokenTimeToLive"]?.let {
                try {
                    refreshTokenTimeToLive(Duration.parse(it as String))
                } catch (e: Exception) {
                    log.warn { "Failed to parse refreshTokenTimeToLive: $it, using default" }
                    refreshTokenTimeToLive(Duration.ofDays(7))
                }
            }
            tokenSettingsMap["reuseRefreshTokens"]?.let {
                try {
                    reuseRefreshTokens(it.toString().toBoolean())
                } catch (e: Exception) {
                    log.warn { "Failed to parse reuseRefreshTokens: $it, using default" }
                    reuseRefreshTokens(false)
                }
            }
        }.build()
        builder.tokenSettings(tokenSettings)
        
        return builder.build()
    }
}