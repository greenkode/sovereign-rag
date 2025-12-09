package ai.sovereignrag.knowledgebase.knowledgesource.repository

import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import ai.sovereignrag.knowledgebase.knowledgesource.domain.KnowledgeSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface KnowledgeSourceRepository : JpaRepository<KnowledgeSource, UUID> {

    fun findByIdAndKnowledgeBaseIdAndStatusNot(
        id: UUID,
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus
    ): KnowledgeSource?

    fun findByKnowledgeBaseIdAndStatusNotOrderByCreatedAtDesc(
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus,
        pageable: Pageable
    ): Page<KnowledgeSource>

    fun findByKnowledgeBaseIdAndStatusOrderByCreatedAtDesc(
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus
    ): List<KnowledgeSource>

    fun findByIngestionJobId(ingestionJobId: UUID): KnowledgeSource?

    fun countByKnowledgeBaseIdAndStatusNot(
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus
    ): Long

    fun countByKnowledgeBaseIdAndStatus(
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus
    ): Long

    @Modifying
    @Query("""
        UPDATE KnowledgeSource k
        SET k.status = :status,
            k.errorMessage = :errorMessage,
            k.updatedAt = :updatedAt
        WHERE k.id = :sourceId AND k.knowledgeBaseId = :knowledgeBaseId
    """)
    fun updateStatus(
        sourceId: UUID,
        knowledgeBaseId: UUID,
        status: KnowledgeSourceStatus,
        errorMessage: String?,
        updatedAt: Instant
    )

    @Modifying
    @Query("""
        UPDATE KnowledgeSource k
        SET k.chunkCount = :chunkCount,
            k.embeddingCount = :embeddingCount,
            k.updatedAt = :updatedAt
        WHERE k.id = :sourceId AND k.knowledgeBaseId = :knowledgeBaseId
    """)
    fun updateEmbeddingStats(
        sourceId: UUID,
        knowledgeBaseId: UUID,
        chunkCount: Int,
        embeddingCount: Int,
        updatedAt: Instant
    )

    @Modifying
    @Query("""
        UPDATE KnowledgeSource k
        SET k.status = ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus.READY,
            k.embeddingCount = :embeddingCount,
            k.processedAt = :processedAt,
            k.updatedAt = :updatedAt
        WHERE k.id = :sourceId AND k.knowledgeBaseId = :knowledgeBaseId
    """)
    fun markReady(
        sourceId: UUID,
        knowledgeBaseId: UUID,
        embeddingCount: Int,
        processedAt: Instant,
        updatedAt: Instant
    )

    @Modifying
    @Query("""
        UPDATE KnowledgeSource k
        SET k.status = ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus.DELETED,
            k.updatedAt = :updatedAt
        WHERE k.id = :sourceId AND k.knowledgeBaseId = :knowledgeBaseId
    """)
    fun softDelete(sourceId: UUID, knowledgeBaseId: UUID, updatedAt: Instant)

    @Modifying
    @Query("""
        UPDATE KnowledgeSource k
        SET k.status = ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus.DELETED,
            k.updatedAt = :updatedAt
        WHERE k.knowledgeBaseId = :knowledgeBaseId
    """)
    fun softDeleteByKnowledgeBase(knowledgeBaseId: UUID, updatedAt: Instant)

    @Query("""
        SELECT COALESCE(SUM(k.embeddingCount), 0)
        FROM KnowledgeSource k
        WHERE k.knowledgeBaseId = :knowledgeBaseId
        AND k.status != ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus.DELETED
    """)
    fun sumEmbeddingCountByKnowledgeBase(knowledgeBaseId: UUID): Long
}
