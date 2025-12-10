package ai.sovereignrag.knowledgebase.knowledgesource.controller

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceInfo
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourcePage
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.commons.knowledgesource.UpdateKnowledgeSourceRequest
import ai.sovereignrag.commons.security.IsService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/knowledge-bases/{knowledgeBaseId}/sources")
class InternalKnowledgeSourceController(
    private val knowledgeSourceGateway: KnowledgeSourceGateway
) {

    @PostMapping
    @IsService
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable knowledgeBaseId: UUID,
        @RequestBody request: CreateKnowledgeSourceRequestDto
    ): KnowledgeSourceResponseDto {
        log.debug { "Internal API: Creating knowledge source for KB: $knowledgeBaseId" }
        val result = knowledgeSourceGateway.create(knowledgeBaseId, request.toRequest())
        return result.toResponseDto()
    }

    @PutMapping("/{sourceId}")
    @IsService
    fun update(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID,
        @RequestBody request: UpdateKnowledgeSourceRequestDto
    ): KnowledgeSourceResponseDto {
        log.debug { "Internal API: Updating knowledge source $sourceId" }
        val result = knowledgeSourceGateway.update(knowledgeBaseId, sourceId, request.toRequest())
        return result.toResponseDto()
    }

    @PatchMapping("/{sourceId}/status")
    @IsService
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateStatus(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID,
        @RequestBody request: UpdateStatusRequestDto
    ) {
        log.debug { "Internal API: Updating status for source $sourceId to ${request.status}" }
        knowledgeSourceGateway.updateStatus(knowledgeBaseId, sourceId, request.status, request.errorMessage)
    }

    @PatchMapping("/{sourceId}/embedding-stats")
    @IsService
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateEmbeddingStats(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID,
        @RequestBody request: UpdateEmbeddingStatsRequestDto
    ) {
        log.debug { "Internal API: Updating embedding stats for source $sourceId" }
        knowledgeSourceGateway.updateEmbeddingStats(knowledgeBaseId, sourceId, request.chunkCount, request.embeddingCount)
    }

    @GetMapping("/{sourceId}")
    @IsService
    fun findById(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID
    ): KnowledgeSourceResponseDto? {
        log.debug { "Internal API: Finding knowledge source $sourceId" }
        return knowledgeSourceGateway.findById(knowledgeBaseId, sourceId)?.toResponseDto()
    }

    @GetMapping
    @IsService
    fun findByKnowledgeBase(
        @PathVariable knowledgeBaseId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): KnowledgeSourcePageDto {
        log.debug { "Internal API: Finding knowledge sources for KB: $knowledgeBaseId, page: $page" }
        return knowledgeSourceGateway.findByKnowledgeBase(knowledgeBaseId, page, size).toDto()
    }

    @GetMapping("/by-status/{status}")
    @IsService
    fun findByStatus(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable status: KnowledgeSourceStatus
    ): List<KnowledgeSourceResponseDto> {
        log.debug { "Internal API: Finding knowledge sources by status $status for KB: $knowledgeBaseId" }
        return knowledgeSourceGateway.findByStatus(knowledgeBaseId, status).map { it.toResponseDto() }
    }

    @DeleteMapping("/{sourceId}")
    @IsService
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable sourceId: UUID
    ) {
        log.debug { "Internal API: Deleting knowledge source $sourceId" }
        knowledgeSourceGateway.delete(knowledgeBaseId, sourceId)
    }

    @GetMapping("/count")
    @IsService
    fun countByKnowledgeBase(@PathVariable knowledgeBaseId: UUID): CountResponseDto {
        return CountResponseDto(knowledgeSourceGateway.countByKnowledgeBase(knowledgeBaseId))
    }

    @GetMapping("/count/by-status/{status}")
    @IsService
    fun countByStatus(
        @PathVariable knowledgeBaseId: UUID,
        @PathVariable status: KnowledgeSourceStatus
    ): CountResponseDto {
        return CountResponseDto(knowledgeSourceGateway.countByStatus(knowledgeBaseId, status))
    }
}

data class CreateKnowledgeSourceRequestDto(
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val s3Key: String?,
    val ingestionJobId: UUID?,
    val metadata: Map<String, Any> = emptyMap()
) {
    fun toRequest() = CreateKnowledgeSourceRequest(
        sourceType = sourceType,
        fileName = fileName,
        sourceUrl = sourceUrl,
        title = title,
        mimeType = mimeType,
        fileSize = fileSize,
        s3Key = s3Key,
        ingestionJobId = ingestionJobId,
        metadata = metadata
    )
}

data class UpdateKnowledgeSourceRequestDto(
    val title: String?,
    val metadata: Map<String, Any>?
) {
    fun toRequest() = UpdateKnowledgeSourceRequest(title = title, metadata = metadata)
}

data class UpdateStatusRequestDto(
    val status: KnowledgeSourceStatus,
    val errorMessage: String?
)

data class UpdateEmbeddingStatsRequestDto(
    val chunkCount: Int,
    val embeddingCount: Int
)

data class KnowledgeSourceResponseDto(
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
)

data class KnowledgeSourcePageDto(
    val content: List<KnowledgeSourceResponseDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class CountResponseDto(val count: Long)

private fun KnowledgeSourceInfo.toResponseDto() = KnowledgeSourceResponseDto(
    id = id,
    knowledgeBaseId = knowledgeBaseId,
    sourceType = sourceType,
    fileName = fileName,
    sourceUrl = sourceUrl,
    title = title,
    mimeType = mimeType,
    fileSize = fileSize,
    s3Key = s3Key,
    status = status,
    errorMessage = errorMessage,
    chunkCount = chunkCount,
    embeddingCount = embeddingCount,
    ingestionJobId = ingestionJobId,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    processedAt = processedAt
)

private fun KnowledgeSourcePage.toDto() = KnowledgeSourcePageDto(
    content = content.map { it.toResponseDto() },
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages
)
