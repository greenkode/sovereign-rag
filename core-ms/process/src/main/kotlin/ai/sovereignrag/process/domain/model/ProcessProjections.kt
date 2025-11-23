package ai.sovereignrag.process.domain.model

import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class ProcessBasicInfo(
    val id: Long,
    val publicId: UUID,
    val type: ProcessType,
    val state: ProcessState,
    val channel: ProcessChannel,
    val createdDate: Instant,
    val externalReference: String?
) : Serializable

data class ProcessTransitionInfo(
    val id: Long,
    val processId: Long,
    val event: ProcessEvent,
    val userId: UUID,
    val oldState: ProcessState,
    val newState: ProcessState,
    val createdDate: Instant
) : Serializable