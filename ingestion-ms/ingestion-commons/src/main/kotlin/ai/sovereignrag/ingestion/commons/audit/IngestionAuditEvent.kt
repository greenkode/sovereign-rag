package ai.sovereignrag.ingestion.commons.audit

import java.time.Instant
import java.util.UUID

data class IngestionAuditEvent(
    val actorId: String,
    val actorName: String,
    val organizationId: UUID,
    val identityType: IngestionIdentityType,
    val resource: IngestionAuditResource,
    val event: IngestionEventType,
    val eventTime: Instant,
    val jobId: UUID? = null,
    val knowledgeBaseId: UUID? = null,
    val payload: Map<String, String> = emptyMap()
)
