package ai.sovereignrag.knowledgebase.knowledgesource.service

import ai.sovereignrag.commons.knowledgesource.CreateKnowledgeSourceRequest
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceGateway
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceInfo
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceNotFoundException
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourcePage
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.commons.knowledgesource.UpdateKnowledgeSourceRequest
import ai.sovereignrag.knowledgebase.knowledgesource.domain.KnowledgeSource
import ai.sovereignrag.knowledgebase.knowledgesource.repository.KnowledgeSourceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional(transactionManager = "masterTransactionManager")
class KnowledgeSourceService(
    private val knowledgeSourceRepository: KnowledgeSourceRepository
) : KnowledgeSourceGateway {

    override fun create(knowledgeBaseId: UUID, request: CreateKnowledgeSourceRequest): KnowledgeSourceInfo {
        log.info { "Creating knowledge source for KB: $knowledgeBaseId, type: ${request.sourceType}" }

        val source = KnowledgeSource(
            knowledgeBaseId = knowledgeBaseId,
            sourceType = request.sourceType,
            fileName = request.fileName,
            sourceUrl = request.sourceUrl,
            title = request.title ?: request.fileName ?: request.sourceUrl,
            mimeType = request.mimeType,
            fileSize = request.fileSize,
            s3Key = request.s3Key,
            ingestionJobId = request.ingestionJobId,
            metadata = request.metadata,
            status = KnowledgeSourceStatus.PENDING
        )

        return knowledgeSourceRepository.save(source).also {
            log.info { "Created knowledge source: ${it.id} for KB: $knowledgeBaseId" }
        }
    }

    override fun update(
        knowledgeBaseId: UUID,
        sourceId: UUID,
        request: UpdateKnowledgeSourceRequest
    ): KnowledgeSourceInfo {
        val source = findSourceOrThrow(knowledgeBaseId, sourceId)

        val updated = source.copy(
            title = request.title ?: source.title,
            metadata = request.metadata ?: source.metadata,
            updatedAt = Instant.now()
        )

        return knowledgeSourceRepository.save(updated)
    }

    override fun updateStatus(
        knowledgeBaseId: UUID,
        sourceId: UUID,
        status: KnowledgeSourceStatus,
        errorMessage: String?
    ) {
        log.info { "Updating source $sourceId status to $status" }
        knowledgeSourceRepository.updateStatus(
            sourceId = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            status = status,
            errorMessage = errorMessage,
            updatedAt = Instant.now()
        )
    }

    override fun updateEmbeddingStats(
        knowledgeBaseId: UUID,
        sourceId: UUID,
        chunkCount: Int,
        embeddingCount: Int
    ) {
        log.debug { "Updating embedding stats for source $sourceId: chunks=$chunkCount, embeddings=$embeddingCount" }
        knowledgeSourceRepository.updateEmbeddingStats(
            sourceId = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            chunkCount = chunkCount,
            embeddingCount = embeddingCount,
            updatedAt = Instant.now()
        )
    }

    override fun findById(knowledgeBaseId: UUID, sourceId: UUID): KnowledgeSourceInfo? {
        return knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
            id = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED
        )
    }

    override fun findByKnowledgeBase(knowledgeBaseId: UUID, page: Int, size: Int): KnowledgeSourcePage {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val pageResult = knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
            knowledgeBaseId = knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED,
            pageable = pageable
        )

        return KnowledgeSourcePage(
            content = pageResult.content,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    override fun findByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): List<KnowledgeSourceInfo> {
        return knowledgeSourceRepository.findByKnowledgeBaseIdAndStatusOrderByCreatedAtDesc(
            knowledgeBaseId = knowledgeBaseId,
            status = status
        )
    }

    override fun delete(knowledgeBaseId: UUID, sourceId: UUID) {
        log.info { "Soft deleting knowledge source: $sourceId from KB: $knowledgeBaseId" }
        knowledgeSourceRepository.softDelete(sourceId, knowledgeBaseId, Instant.now())
    }

    override fun deleteByKnowledgeBase(knowledgeBaseId: UUID) {
        log.info { "Soft deleting all knowledge sources for KB: $knowledgeBaseId" }
        knowledgeSourceRepository.softDeleteByKnowledgeBase(knowledgeBaseId, Instant.now())
    }

    override fun countByKnowledgeBase(knowledgeBaseId: UUID): Long {
        return knowledgeSourceRepository.countByKnowledgeBaseIdAndStatusNot(
            knowledgeBaseId = knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED
        )
    }

    override fun countByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): Long {
        return knowledgeSourceRepository.countByKnowledgeBaseIdAndStatus(knowledgeBaseId, status)
    }

    fun findByIngestionJobId(ingestionJobId: UUID): KnowledgeSource? {
        return knowledgeSourceRepository.findByIngestionJobId(ingestionJobId)
    }

    fun markReady(knowledgeBaseId: UUID, sourceId: UUID, embeddingCount: Int) {
        val now = Instant.now()
        knowledgeSourceRepository.markReady(
            sourceId = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            embeddingCount = embeddingCount,
            processedAt = now,
            updatedAt = now
        )
        log.info { "Knowledge source $sourceId marked as READY with $embeddingCount embeddings" }
    }

    fun getTotalEmbeddingCount(knowledgeBaseId: UUID): Long {
        return knowledgeSourceRepository.sumEmbeddingCountByKnowledgeBase(knowledgeBaseId)
    }

    private fun findSourceOrThrow(knowledgeBaseId: UUID, sourceId: UUID): KnowledgeSource {
        return knowledgeSourceRepository.findByIdAndKnowledgeBaseIdAndStatusNot(
            id = sourceId,
            knowledgeBaseId = knowledgeBaseId,
            status = KnowledgeSourceStatus.DELETED
        ) ?: throw KnowledgeSourceNotFoundException("Knowledge source not found: $sourceId")
    }
}
