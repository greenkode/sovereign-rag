package ai.sovereignrag.audit.domain.model

import java.time.Instant
import java.util.UUID

data class AuditLog(
    val id: UUID,
    val actorId: String,
    val actorName: String,
    val merchantId: String,
    val resource: String,
    val identityType: String,
    val event: String,
    val eventTime: Instant,
    val timeRecorded: Instant,
    val payload: Map<String, Any>,
    val ipAddress: String = "N/A"
)