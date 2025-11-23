package ai.sovereignrag.client.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "unanswered_queries", schema = "core")
@EntityListeners(AuditingEntityListener::class)
data class UnansweredQuery(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, columnDefinition = "TEXT")
    val query: String = "",

    @Column(columnDefinition = "TEXT")
    val response: String? = null,

    val language: String? = null,

    @Column(name = "session_id")
    val sessionId: UUID? = null,

    @Column(name = "confidence_score")
    val confidenceScore: Double? = null,

    val reason: String? = null,

    val status: String = "open",

    @Column(name = "used_general_knowledge")
    val usedGeneralKnowledge: Boolean = false,

    @Column(name = "resolved_at")
    val resolvedAt: Instant? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "occurrence_count", nullable = false)
    var occurrenceCount: Int = 1,

    @LastModifiedDate
    @Column(name = "last_occurred_at", nullable = false)
    var lastOccurredAt: Instant = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnansweredQuery) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "UnansweredQuery(id=$id, query='${query.take(50)}...', status='$status')"
    }
}
