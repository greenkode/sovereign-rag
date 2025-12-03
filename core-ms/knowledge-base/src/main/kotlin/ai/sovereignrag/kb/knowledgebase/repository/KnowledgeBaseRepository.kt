package ai.sovereignrag.kb.knowledgebase.repository

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.kb.knowledgebase.domain.KnowledgeBase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface KnowledgeBaseRepository : JpaRepository<KnowledgeBase, String> {

    fun findByIdAndDeletedAtIsNull(id: String): KnowledgeBase?

    fun findByDeletedAtIsNullOrderByCreatedAtDesc(): List<KnowledgeBase>

    fun findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status: KnowledgeBaseStatus): List<KnowledgeBase>

    fun existsByIdAndDeletedAtIsNull(id: String): Boolean

    fun existsBySchemaName(schemaName: String): Boolean

    fun findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId: UUID): List<KnowledgeBase>

    fun findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
        organizationId: UUID,
        status: KnowledgeBaseStatus
    ): List<KnowledgeBase>

    fun countByOrganizationIdAndDeletedAtIsNull(organizationId: UUID): Long

    fun findByOauthClientIdAndDeletedAtIsNull(oauthClientId: String): KnowledgeBase?

    @Modifying
    @Query("""
        UPDATE KnowledgeBase k
        SET k.lastActiveAt = :timestamp
        WHERE k.id = :knowledgeBaseId
    """)
    fun updateLastActive(knowledgeBaseId: String, timestamp: Instant)

    @Modifying
    @Query("""
        UPDATE KnowledgeBase k
        SET k.oauthClientId = :oauthClientId,
            k.updatedAt = :updatedAt
        WHERE k.id = :knowledgeBaseId
    """)
    fun updateOauthClientId(knowledgeBaseId: String, oauthClientId: String, updatedAt: Instant)

    @Modifying
    @Query("""
        UPDATE KnowledgeBase k
        SET k.status = ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus.DELETED,
            k.deletedAt = :deletedAt,
            k.updatedAt = :updatedAt
        WHERE k.id = :knowledgeBaseId
    """)
    fun softDelete(knowledgeBaseId: String, deletedAt: Instant, updatedAt: Instant)
}
