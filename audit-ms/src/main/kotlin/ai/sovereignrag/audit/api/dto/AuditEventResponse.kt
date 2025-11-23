package ai.sovereignrag.audit.api.dto

import java.time.LocalDateTime

class AuditEventResponse(
    val actorId: String,
    val actorName: String,
    val merchantId: String,
    val identityType: String,
    val resource: String,
    val eventType: String,
    val eventTime: LocalDateTime,
    val timeRecorded: LocalDateTime,
    val payload: Map<String, Any> = mapOf(),
    val ipAddress: String = "N/A"
)
