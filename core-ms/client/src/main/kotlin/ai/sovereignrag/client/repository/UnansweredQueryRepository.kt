package ai.sovereignrag.client.repository

import nl.compilot.ai.client.domain.UnansweredQuery
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UnansweredQueryRepository : JpaRepository<UnansweredQuery, UUID> {

    /**
     * Get all unanswered queries ordered by occurrence count (most frequent first)
     */
    fun findAllByOrderByOccurrenceCountDescLastOccurredAtDesc(): List<UnansweredQuery>

    /**
     * Get only open (unreviewed) queries ordered by occurrence count
     */
    fun findByStatusOrderByOccurrenceCountDescLastOccurredAtDesc(status: String): List<UnansweredQuery>

    /**
     * Get paginated queries by status ordered by occurrence count
     */
    fun findByStatus(status: String, pageable: Pageable): Page<UnansweredQuery>

    /**
     * Count queries by status
     */
    fun countByStatus(status: String): Long

    /**
     * Find similar queries by query text
     */
    @Query("""
        SELECT u FROM UnansweredQuery u
        WHERE LOWER(u.query) LIKE LOWER(CONCAT('%', :queryText, '%'))
        ORDER BY u.occurrenceCount DESC, u.lastOccurredAt DESC
    """)
    fun findSimilarQueries(queryText: String): List<UnansweredQuery>

    /**
     * Find by query text (exact match for deduplication)
     */
    fun findByQuery(query: String): UnansweredQuery?

    /**
     * Atomically insert or update unanswered query using native SQL.
     * This avoids optimistic locking issues in concurrent/async environments.
     * Uses PostgreSQL's INSERT ... ON CONFLICT to handle race conditions at database level.
     */
    @Modifying
    @Query(value = """
        INSERT INTO unanswered_queries
            (id, query, response, confidence_score, used_general_knowledge, language, status,
             occurrence_count, created_at, last_occurred_at, version)
        VALUES
            (gen_random_uuid(), :query, :response, :confidenceScore, :usedGeneralKnowledge,
             :language, 'open', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
        ON CONFLICT (query)
        DO UPDATE SET
            occurrence_count = unanswered_queries.occurrence_count + 1,
            last_occurred_at = CURRENT_TIMESTAMP,
            version = unanswered_queries.version + 1
    """, nativeQuery = true)
    fun upsertUnansweredQuery(
        query: String,
        response: String?,
        confidenceScore: Double?,
        usedGeneralKnowledge: Boolean,
        language: String?
    ): Int
}
