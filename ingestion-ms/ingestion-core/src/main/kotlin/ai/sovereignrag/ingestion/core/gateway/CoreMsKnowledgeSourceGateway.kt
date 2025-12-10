package ai.sovereignrag.ingestion.core.gateway

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceInfo
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceNotFoundException
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourcePage
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.commons.knowledgesource.UpdateKnowledgeSourceRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CoreMsKnowledgeSourceGateway(
    @Value("\${core-ms.base-url:http://localhost:9082}") private val coreMsBaseUrl: String,
    private val serviceAuthorizedClientManager: AuthorizedClientServiceOAuth2AuthorizedClientManager
) : KnowledgeSourceGateway {

    companion object {
        private const val CLIENT_REGISTRATION_ID = "ingestion-ms-client"
    }

    private val restClient = RestClient.builder()
        .baseUrl(coreMsBaseUrl)
        .build()

    override fun create(knowledgeBaseId: UUID, request: CreateKnowledgeSourceRequest): KnowledgeSourceInfo {
        log.debug { "Creating knowledge source in core-ms for KB: $knowledgeBaseId" }

        val accessToken = getAccessToken()
            ?: throw RuntimeException("Unable to authenticate with core-ms")

        val requestDto = mapOf(
            "sourceType" to request.sourceType.name,
            "fileName" to request.fileName,
            "sourceUrl" to request.sourceUrl,
            "title" to request.title,
            "mimeType" to request.mimeType,
            "fileSize" to request.fileSize,
            "s3Key" to request.s3Key,
            "ingestionJobId" to request.ingestionJobId?.toString(),
            "metadata" to request.metadata
        )

        return restClient.post()
            .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources", knowledgeBaseId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(requestDto)
            .retrieve()
            .body(KnowledgeSourceDto::class.java)
            ?.toInfo()
            ?: throw RuntimeException("Failed to create knowledge source")
    }

    override fun update(knowledgeBaseId: UUID, sourceId: UUID, request: UpdateKnowledgeSourceRequest): KnowledgeSourceInfo {
        log.debug { "Updating knowledge source $sourceId in core-ms" }

        val accessToken = getAccessToken()
            ?: throw RuntimeException("Unable to authenticate with core-ms")

        val requestDto = mapOf(
            "title" to request.title,
            "metadata" to request.metadata
        )

        return restClient.put()
            .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/{sourceId}", knowledgeBaseId, sourceId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(requestDto)
            .retrieve()
            .body(KnowledgeSourceDto::class.java)
            ?.toInfo()
            ?: throw KnowledgeSourceNotFoundException("Knowledge source not found: $sourceId")
    }

    override fun updateStatus(knowledgeBaseId: UUID, sourceId: UUID, status: KnowledgeSourceStatus, errorMessage: String?) {
        log.debug { "Updating status for source $sourceId to $status in core-ms" }

        val accessToken = getAccessToken() ?: return

        runCatching {
            restClient.patch()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/{sourceId}/status", knowledgeBaseId, sourceId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mapOf("status" to status.name, "errorMessage" to errorMessage))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e ->
            log.error(e) { "Failed to update status for source $sourceId" }
        }
    }

    override fun updateEmbeddingStats(knowledgeBaseId: UUID, sourceId: UUID, chunkCount: Int, embeddingCount: Int) {
        log.debug { "Updating embedding stats for source $sourceId in core-ms" }

        val accessToken = getAccessToken() ?: return

        runCatching {
            restClient.patch()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/{sourceId}/embedding-stats", knowledgeBaseId, sourceId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mapOf("chunkCount" to chunkCount, "embeddingCount" to embeddingCount))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e ->
            log.error(e) { "Failed to update embedding stats for source $sourceId" }
        }
    }

    override fun findById(knowledgeBaseId: UUID, sourceId: UUID): KnowledgeSourceInfo? {
        log.debug { "Finding knowledge source $sourceId from core-ms" }

        val accessToken = getAccessToken() ?: return null

        return runCatching {
            restClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/{sourceId}", knowledgeBaseId, sourceId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(KnowledgeSourceDto::class.java)
                ?.toInfo()
        }.onFailure { e ->
            when {
                e is HttpClientErrorException && e.statusCode == HttpStatus.NOT_FOUND -> {
                    log.debug { "Knowledge source not found: $sourceId" }
                }
                else -> log.error(e) { "Failed to find knowledge source: $sourceId" }
            }
        }.getOrNull()
    }

    override fun findByKnowledgeBase(knowledgeBaseId: UUID, page: Int, size: Int): KnowledgeSourcePage {
        log.debug { "Finding knowledge sources for KB $knowledgeBaseId from core-ms" }

        val accessToken = getAccessToken()
            ?: return KnowledgeSourcePage(emptyList(), page, size, 0, 0)

        return runCatching {
            restClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources?page={page}&size={size}", knowledgeBaseId, page, size)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(KnowledgeSourcePageDto::class.java)
                ?.toPage()
                ?: KnowledgeSourcePage(emptyList(), page, size, 0, 0)
        }.getOrElse { e ->
            log.error(e) { "Failed to find knowledge sources for KB: $knowledgeBaseId" }
            KnowledgeSourcePage(emptyList(), page, size, 0, 0)
        }
    }

    override fun findByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): List<KnowledgeSourceInfo> {
        log.debug { "Finding knowledge sources by status $status for KB $knowledgeBaseId from core-ms" }

        val accessToken = getAccessToken() ?: return emptyList()

        return runCatching {
            restClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/by-status/{status}", knowledgeBaseId, status)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<KnowledgeSourceDto>>() {})
                ?.map { it.toInfo() }
                ?: emptyList()
        }.getOrElse { e ->
            log.error(e) { "Failed to find knowledge sources by status for KB: $knowledgeBaseId" }
            emptyList()
        }
    }

    override fun delete(knowledgeBaseId: UUID, sourceId: UUID) {
        log.debug { "Deleting knowledge source $sourceId from core-ms" }

        val accessToken = getAccessToken() ?: return

        runCatching {
            restClient.delete()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/{sourceId}", knowledgeBaseId, sourceId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e ->
            log.error(e) { "Failed to delete knowledge source: $sourceId" }
        }
    }

    override fun deleteByKnowledgeBase(knowledgeBaseId: UUID) {
        log.warn { "deleteByKnowledgeBase not implemented - should be called via core-ms directly" }
    }

    override fun countByKnowledgeBase(knowledgeBaseId: UUID): Long {
        log.debug { "Counting knowledge sources for KB $knowledgeBaseId from core-ms" }

        val accessToken = getAccessToken() ?: return 0

        return runCatching {
            restClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/count", knowledgeBaseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(CountDto::class.java)
                ?.count ?: 0
        }.getOrElse { e ->
            log.error(e) { "Failed to count knowledge sources for KB: $knowledgeBaseId" }
            0
        }
    }

    override fun countByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): Long {
        log.debug { "Counting knowledge sources by status $status for KB $knowledgeBaseId from core-ms" }

        val accessToken = getAccessToken() ?: return 0

        return runCatching {
            restClient.get()
                .uri("/internal/knowledge-bases/{knowledgeBaseId}/sources/count/by-status/{status}", knowledgeBaseId, status)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(CountDto::class.java)
                ?.count ?: 0
        }.getOrElse { e ->
            log.error(e) { "Failed to count knowledge sources by status for KB: $knowledgeBaseId" }
            0
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

private data class KnowledgeSourceDto(
    val id: UUID,
    val knowledgeBaseId: UUID,
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val s3Key: String?,
    val status: KnowledgeSourceStatus,
    val errorMessage: String?,
    val chunkCount: Int,
    val embeddingCount: Int,
    val ingestionJobId: UUID?,
    val metadata: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val processedAt: Instant?
) {
    fun toInfo(): KnowledgeSourceInfo = object : KnowledgeSourceInfo {
        override val id: UUID = this@KnowledgeSourceDto.id
        override val knowledgeBaseId: UUID = this@KnowledgeSourceDto.knowledgeBaseId
        override val sourceType: SourceType = this@KnowledgeSourceDto.sourceType
        override val fileName: String? = this@KnowledgeSourceDto.fileName
        override val sourceUrl: String? = this@KnowledgeSourceDto.sourceUrl
        override val title: String? = this@KnowledgeSourceDto.title
        override val mimeType: String? = this@KnowledgeSourceDto.mimeType
        override val fileSize: Long? = this@KnowledgeSourceDto.fileSize
        override val s3Key: String? = this@KnowledgeSourceDto.s3Key
        override val status: KnowledgeSourceStatus = this@KnowledgeSourceDto.status
        override val errorMessage: String? = this@KnowledgeSourceDto.errorMessage
        override val chunkCount: Int = this@KnowledgeSourceDto.chunkCount
        override val embeddingCount: Int = this@KnowledgeSourceDto.embeddingCount
        override val ingestionJobId: UUID? = this@KnowledgeSourceDto.ingestionJobId
        override val metadata: Map<String, Any> = this@KnowledgeSourceDto.metadata
        override val createdAt: Instant = this@KnowledgeSourceDto.createdAt
        override val updatedAt: Instant = this@KnowledgeSourceDto.updatedAt
        override val processedAt: Instant? = this@KnowledgeSourceDto.processedAt
    }
}

private data class KnowledgeSourcePageDto(
    val content: List<KnowledgeSourceDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    fun toPage() = KnowledgeSourcePage(
        content = content.map { it.toInfo() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages
    )
}

private data class CountDto(val count: Long)
