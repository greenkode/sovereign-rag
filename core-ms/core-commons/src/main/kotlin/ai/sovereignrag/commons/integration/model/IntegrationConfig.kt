package ai.sovereignrag.commons.integration.model

import ai.sovereignrag.commons.enumeration.IntegrationConfigStatus
import java.time.Instant
import java.util.UUID

data class IntegrationConfig(
    val id: Long,
    val publicId: UUID,
    val identifier: String,
    val priority: Int,
    val exchangeId: String,
    val start: Instant,
    val expiry: Instant?,
    val action: IntegrationConfigAction,
    val status: IntegrationConfigStatus,
)