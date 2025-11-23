package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
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
    private val clientRepository: OAuthRegisteredClientRepository,
    private val objectMapper: ObjectMapper
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
        
        // Check and unlock if lockout has expired
        if (client != null && client.checkAndUnlockIfExpired()) {
            clientRepository.save(client)
            log.info { "Lockout expired for client: ${client.clientId}, client unlocked" }
        }
        
        // Return the client regardless of lock status - let authentication provider handle lockout
        return client?.let { mapToRegisteredClient(it) }
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