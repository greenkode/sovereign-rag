package ai.sovereignrag.identity.commons.audit

import java.time.Instant

data class AuditEvent(
    val actorId: String,
    val actorName: String,
    val merchantId: String,
    val identityType: IdentityType,
    val resource: AuditResource,
    val event: String,
    val eventTime: Instant,
    val timeRecorded: Instant,
    val payload: Map<String, String> = emptyMap(),
)

