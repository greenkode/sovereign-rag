package ai.sovereignrag.ingestion.core.gateway

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.commons.embedding.EmbeddingModelGateway
import ai.sovereignrag.commons.embedding.EmbeddingModelNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CoreMsEmbeddingModelGateway(
    @Value("\${core-ms.base-url:http://localhost:9082}") private val coreMsBaseUrl: String,
    private val serviceAuthorizedClientManager: AuthorizedClientServiceOAuth2AuthorizedClientManager
) : EmbeddingModelGateway {

    companion object {
        private const val CLIENT_REGISTRATION_ID = "ingestion-ms-client"
    }

    private val restClient = RestClient.builder()
        .baseUrl(coreMsBaseUrl)
        .build()

    override fun findById(modelId: String): EmbeddingModelConfig? {
        log.debug { "Fetching embedding model by id from core-ms: $modelId" }

        val accessToken = getAccessToken() ?: return null

        return runCatching {
            restClient.get()
                .uri("/internal/embedding-models/{modelId}", modelId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(EmbeddingModelConfigDto::class.java)
                ?.toConfig()
        }.onFailure { e ->
            when {
                e is HttpClientErrorException && e.statusCode == HttpStatus.NOT_FOUND -> {
                    log.debug { "Embedding model not found: $modelId" }
                }
                else -> log.error(e) { "Failed to fetch embedding model from core-ms: $modelId" }
            }
        }.getOrNull()
    }

    override fun findByKnowledgeBase(knowledgeBaseId: UUID): EmbeddingModelConfig? {
        log.debug { "Fetching embedding model for knowledge base from core-ms: $knowledgeBaseId" }

        val accessToken = getAccessToken() ?: return null

        return runCatching {
            restClient.get()
                .uri("/internal/embedding-models/knowledge-base/{knowledgeBaseId}", knowledgeBaseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(EmbeddingModelConfigDto::class.java)
                ?.toConfig()
        }.onFailure { e ->
            when {
                e is HttpClientErrorException && e.statusCode == HttpStatus.NOT_FOUND -> {
                    log.debug { "Embedding model not found for knowledge base: $knowledgeBaseId" }
                }
                else -> log.error(e) { "Failed to fetch embedding model from core-ms for knowledge base: $knowledgeBaseId" }
            }
        }.getOrNull()
    }

    override fun getDefault(): EmbeddingModelConfig {
        log.debug { "Fetching default embedding model from core-ms" }

        val accessToken = getAccessToken()
            ?: throw EmbeddingModelNotFoundException("Unable to authenticate with core-ms")

        return runCatching {
            restClient.get()
                .uri("/internal/embedding-models/default")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(EmbeddingModelConfigDto::class.java)
                ?.toConfig()
                ?: throw EmbeddingModelNotFoundException("default")
        }.getOrElse { e ->
            log.error(e) { "Failed to fetch default embedding model from core-ms" }
            throw EmbeddingModelNotFoundException("default")
        }
    }

    private fun getAccessToken(): String? =
        runCatching {
            val authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal("ingestion-ms")
                .build()

            serviceAuthorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
        }.onFailure { e ->
            log.error(e) { "Failed to obtain OAuth2 access token for core-ms" }
        }.getOrNull()
}

private data class EmbeddingModelConfigDto(
    val id: String,
    val name: String,
    val modelId: String,
    val provider: String,
    val dimensions: Int,
    val maxTokens: Int,
    val baseUrl: String?,
    val apiKey: String? = null
) {
    fun toConfig(): EmbeddingModelConfig = object : EmbeddingModelConfig {
        override val id: String = this@EmbeddingModelConfigDto.id
        override val name: String = this@EmbeddingModelConfigDto.name
        override val modelId: String = this@EmbeddingModelConfigDto.modelId
        override val provider: String = this@EmbeddingModelConfigDto.provider
        override val dimensions: Int = this@EmbeddingModelConfigDto.dimensions
        override val maxTokens: Int = this@EmbeddingModelConfigDto.maxTokens
        override val baseUrl: String? = this@EmbeddingModelConfigDto.baseUrl
        override val apiKey: String? = this@EmbeddingModelConfigDto.apiKey
    }
}
