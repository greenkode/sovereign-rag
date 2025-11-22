package ai.sovereignrag.client.repository

import nl.compilot.ai.client.domain.Escalation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface EscalationRepository : JpaRepository<Escalation, UUID> {

    /**
     * Find escalations by session ID
     */
    fun findBySessionId(sessionId: UUID): List<Escalation>

    /**
     * Find escalations by status
     */
    fun findByStatusOrderByCreatedAtDesc(status: String): List<Escalation>

    /**
     * Count unreviewed escalations
     */
    fun countByReviewed(reviewed: Boolean): Long

    /**
     * Mark escalation as reviewed
     */
    @Modifying
    @Query("""
        UPDATE Escalation e
        SET e.reviewed = true,
            e.reviewedAt = :reviewedAt,
            e.reviewedBy = :reviewedBy,
            e.status = :status,
            e.resolutionNotes = :notes
        WHERE e.id = :id
    """)
    fun markAsReviewed(
        id: UUID,
        reviewedAt: Instant,
        reviewedBy: String,
        status: String,
        notes: String?
    )

    /**
     * Find recent escalations
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<Escalation>
}
