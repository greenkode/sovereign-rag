package ai.sovereignrag.accounting.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AccountingRestClientConfig(
    @Value("\${integration.accounting.base-url}") private val baseUrl: String
) {

    @Bean("accountingRestClient")
    fun restClient(
        @Value("\${integration.accounting.base-url}") baseUrl: String
    ): RestClient {

        return RestClient.builder()
            .baseUrl("$baseUrl/api")
            .build()
    }

    @Bean("accountingAdminRestClient")
    fun adminRestClient(
        @Value("\${integration.accounting.base-url}") baseUrl: String
    ): RestClient {

        return RestClient.builder()
            .baseUrl(baseUrl)
            .build()
    }
}