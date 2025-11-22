package nl.compilot.ai.client.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Tenant-specific escalation entity
 * Stored in tenant databases (uses tenant DataSource routing)
 */
@Entity
@Table(name = "escalations")
data class Escalation(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "session_id")
    val sessionId: UUID,

    @Column(name = "reason", columnDefinition = "TEXT")
    val reason: String,

    @Column(name = "status", length = 50)
    val status: String = "pending",

    @Column(name = "priority", length = 20)
    val priority: String = "normal",

    // User contact info
    @Column(name = "user_email", length = 500)
    val userEmail: String,

    @Column(name = "user_name", length = 500)
    val userName: String? = null,

    @Column(name = "user_phone", length = 100)
    val userPhone: String? = null,

    // Assignment
    @Column(name = "assigned_to", length = 255)
    val assignedTo: String? = null,

    @Column(name = "assigned_at")
    val assignedAt: Instant? = null,

    // Resolution
    @Column(name = "resolved_at")
    val resolvedAt: Instant? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

    // Additional fields for compatibility with EscalationLogger interface
    @Column(name = "user_message", columnDefinition = "TEXT")
    val userMessage: String? = null,

    @Column(name = "language", length = 10)
    val language: String? = null,

    @Column(name = "persona", length = 100)
    val persona: String,

    @Column(name = "email_sent")
    val emailSent: Boolean = false,

    @Column(name = "reviewed")
    val reviewed: Boolean = false,

    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,

    @Column(name = "reviewed_by", length = 255)
    val reviewedBy: String? = null,

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now(),

    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any> = emptyMap()
)
