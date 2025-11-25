package ai.sovereignrag.identity.process.domain.model

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Lightweight projection for process basic info - no collections loaded
 * Use case: Quick status checks, process existence verification
 */
data class ProcessBasicInfo(
    val id: Long,
    val publicId: UUID,
    val type: ProcessType,
    val state: ProcessState,
    val channel: Channel,
    val createdAt: Instant,
    val externalReference: String?
) : Serializable


/**
 * Process transitions only - for audit/history
 */
data class ProcessTransitionInfo(
    val id: Long,
    val processId: Long,
    val event: ProcessEvent,
    val userId: UUID,
    val oldState: ProcessState,
    val newState: ProcessState,
    val createdAt: Instant
) : Serializable