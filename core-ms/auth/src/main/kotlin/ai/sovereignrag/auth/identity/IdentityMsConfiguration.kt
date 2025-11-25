package ai.sovereignrag.auth.identity

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.web.client.RestClient

private val log = logger {}

@Configuration
class IdentityMsConfiguration(
    @Value("\${sovereignrag.identity-ms.base-url:http://localhost:9093}") private val identityBaseUrl: String
) {

    @Bean
    fun identityOAuth2ClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientRepository: OAuth2AuthorizedClientRepository
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        return DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository
        ).apply {
            setAuthorizedClientProvider(authorizedClientProvider)
        }
    }

    @Bean("identityMsRestClient")
    fun identityMsRestClient(identityOAuth2ClientManager: OAuth2AuthorizedClientManager): RestClient {
        val oauth2Interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            val authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("identity-ms")
                .principal("core-ms")
                .build()

            identityOAuth2ClientManager.authorize(authorizeRequest)?.let { authorizedClient ->
                request.headers.setBearerAuth(authorizedClient.accessToken.tokenValue)
                log.debug { "Added OAuth2 Bearer token to Identity MS request" }
            } ?: log.warn { "Failed to obtain OAuth2 token for Identity MS" }

            execution.execute(request, body)
        }

        return RestClient.builder()
            .baseUrl(identityBaseUrl)
            .requestInterceptor(oauth2Interceptor)
            .build()
    }
}
