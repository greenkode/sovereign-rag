package ai.sovereignrag.ingestion.core.gateway

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseGateway
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Component
class CoreMsKnowledgeBaseGateway(
    @Value("\${core-ms.base-url}") private val coreMsBaseUrl: String,
    private val webClientBuilder: WebClient.Builder,
    private val authorizedClientManager: OAuth2AuthorizedClientManager
) : KnowledgeBaseGateway {

    private val webClient: WebClient by lazy {
        webClientBuilder.baseUrl(coreMsBaseUrl).build()
    }

    private val databaseConfigCache = ConcurrentHashMap<String, KnowledgeBaseDatabaseConfig>()

    @Cacheable(cacheNames = ["knowledge_base_info"], key = "#knowledgeBaseId")
    override fun findById(knowledgeBaseId: String): KnowledgeBaseInfo? {
        return fetchKnowledgeBaseInfo(knowledgeBaseId)
    }

    override fun findByOrganization(organizationId: UUID): List<KnowledgeBaseInfo> {
        log.warn { "findByOrganization not implemented for ingestion-ms gateway" }
        return emptyList()
    }

    override fun existsById(knowledgeBaseId: String): Boolean {
        return findById(knowledgeBaseId) != null
    }

    override fun updateStats(knowledgeBaseId: String, knowledgeSourceCount: Int, embeddingCount: Int) {
        log.debug { "Stats update requested for KB $knowledgeBaseId: sources=$knowledgeSourceCount, embeddings=$embeddingCount" }
    }

    @Cacheable(cacheNames = ["knowledge_base_db_config"], key = "#knowledgeBaseId")
    fun getDatabaseConfig(knowledgeBaseId: String): KnowledgeBaseDatabaseConfig {
        return databaseConfigCache.computeIfAbsent(knowledgeBaseId) {
            fetchDatabaseConfig(knowledgeBaseId)
                ?: throw KnowledgeBaseNotFoundException("Knowledge base not found: $knowledgeBaseId")
        }
    }

    fun evictDatabaseConfigCache(knowledgeBaseId: String) {
        databaseConfigCache.remove(knowledgeBaseId)
    }

    private fun fetchKnowledgeBaseInfo(knowledgeBaseId: String): KnowledgeBaseInfo? {
        val accessToken = getAccessToken() ?: return null

        return runCatching {
            webClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}", knowledgeBaseId)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .bodyToMono<KnowledgeBaseInfoDto>()
                .block()
                ?.toInfo()
        }.onFailure { e ->
            log.error(e) { "Failed to fetch knowledge base info for $knowledgeBaseId" }
        }.getOrNull()
    }

    private fun fetchDatabaseConfig(knowledgeBaseId: String): KnowledgeBaseDatabaseConfig? {
        val accessToken = getAccessToken() ?: return null

        return runCatching {
            webClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/database-config", knowledgeBaseId)
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .bodyToMono<KnowledgeBaseDatabaseConfigDto>()
                .block()
                ?.toConfig()
        }.onFailure { e ->
            log.error(e) { "Failed to fetch database config for knowledge base $knowledgeBaseId" }
        }.getOrNull()
    }

    private fun getAccessToken(): String? = runCatching {
        val request = OAuth2AuthorizeRequest
            .withClientRegistrationId("ingestion-ms-client")
            .principal("ingestion-ms")
            .build()

        authorizedClientManager.authorize(request)?.accessToken?.tokenValue
    }.onFailure { e ->
        log.error(e) { "Failed to obtain OAuth2 access token for core-ms" }
    }.getOrNull()
}

private data class KnowledgeBaseInfoDto(
    val id: String,
    val organizationId: UUID,
    val regionCode: String,
    val schemaName: String,
    val status: String,
    val embeddingModelId: String?
) {
    fun toInfo(): KnowledgeBaseInfo = object : KnowledgeBaseInfo {
        override val id: String = this@KnowledgeBaseInfoDto.id
        override val organizationId: UUID = this@KnowledgeBaseInfoDto.organizationId
        override val schemaName: String = this@KnowledgeBaseInfoDto.schemaName
        override val regionCode: String = this@KnowledgeBaseInfoDto.regionCode
        override val status: KnowledgeBaseStatus = KnowledgeBaseStatus.valueOf(this@KnowledgeBaseInfoDto.status)
        override val oauthClientId: String? = null
        override val apiKeyHash: String? = null
        override val embeddingModelId: String? = this@KnowledgeBaseInfoDto.embeddingModelId
    }
}

private data class KnowledgeBaseDatabaseConfigDto(
    val knowledgeBaseId: String,
    val organizationId: UUID,
    val regionCode: String,
    val schemaName: String,
    val databaseName: String,
    val embeddingModelId: String?
) {
    fun toConfig(): KnowledgeBaseDatabaseConfig = KnowledgeBaseDatabaseConfig(
        knowledgeBaseId = knowledgeBaseId,
        organizationId = organizationId,
        regionCode = regionCode,
        schemaName = schemaName,
        databaseName = databaseName,
        embeddingModelId = embeddingModelId
    )
}

data class KnowledgeBaseDatabaseConfig(
    val knowledgeBaseId: String,
    val organizationId: UUID,
    val regionCode: String,
    val schemaName: String,
    val databaseName: String,
    val embeddingModelId: String?
)
