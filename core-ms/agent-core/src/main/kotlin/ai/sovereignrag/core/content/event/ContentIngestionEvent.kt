package ai.sovereignrag.core.content.event

import java.time.Instant
import java.util.UUID

/**
 * Event published when content is successfully ingested
 * Used to trigger contextual enrichment processes
 */
data class ContentIngestionEvent(
    val tenantId: String,
    val documentId: UUID,
    val title: String,
    val category: String?,
    val postType: String,
    val timestamp: Instant = Instant.now()
)
