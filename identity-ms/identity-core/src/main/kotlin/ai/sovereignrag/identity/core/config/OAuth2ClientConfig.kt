package ai.sovereignrag.identity.core.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.web.client.RestClient

@Configuration
class OAuth2ClientConfig {

    private val log = KotlinLogging.logger {}

    @Bean
    @Primary
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientRepository: OAuth2AuthorizedClientRepository
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        val authorizedClientManager = DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository
        )
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

        return authorizedClientManager
    }

    @Bean
    fun authorizedClientService(
        clientRegistrationRepository: ClientRegistrationRepository
    ): OAuth2AuthorizedClientService {
        return InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
    }

    @Bean
    fun serviceAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): AuthorizedClientServiceOAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        val clientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        )
        clientManager.setAuthorizedClientProvider(authorizedClientProvider)

        return clientManager
    }
    
    @Bean
    fun oauth2RestClient(
        @Value("\${core-ms.base-url}") coreMsBaseUrl: String,
        authorizedClientManager: OAuth2AuthorizedClientManager
    ): RestClient {
        val oauth2Interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            val authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("identity-ms-client")
                .principal("identity-ms-client")
                .build()
            
            val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
            
            if (authorizedClient != null) {
                val accessToken = authorizedClient.accessToken.tokenValue
                request.headers.setBearerAuth(accessToken)
                log.debug { "Added OAuth2 Bearer token to request" }
            }
            
            execution.execute(request, body)
        }
        
        return RestClient.builder()
            .baseUrl(coreMsBaseUrl)
            .requestInterceptor(oauth2Interceptor)
            .build()
    }
}