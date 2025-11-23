package ai.sovereignrag.identity.commons.process


import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.exception.ServerException
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import mu.KotlinLogging.logger
import java.time.Instant
import java.util.UUID

data class ProcessDto(
    val id: Long,
    val publicId: UUID,
    val state: ProcessState,
    val type: ProcessType,
    val channel: Channel,
    val createdDate: Instant,
    val requests: List<ProcessRequestDto>,
    val externalReference: String? = null
) {

    fun open(): Boolean = state == ProcessState.PENDING

    fun getInitialRequest(): ProcessRequestDto {
        return requests.first { it.type == ProcessRequestType.CREATE_NEW_PROCESS }
    }

    fun getRequestByType(requestType: ProcessRequestType): ProcessRequestDto {
        return requests.first { it.type == requestType }
    }

    fun getLatestRequestByType(requestType: ProcessRequestType): ProcessRequestDto {
        return requests.sortedBy { it -> it.id }.last { it.type == requestType }
    }
}

data class ProcessRequestDto(
    val id: Long,
    val type: ProcessRequestType,
    val state: ProcessState,
    val stakeholders: Map<ProcessStakeholderType, String>,
    val data: Map<ProcessRequestDataName, String>
) {

    private val log = logger {}

    fun getDataValue(dataName: ProcessRequestDataName): String = data[dataName] ?: run {
        log.error { "No data found for $dataName, request id: $id" }
        throw ServerException(
            "Missing process data!!"
        )
    }

    fun getDataValueOrNull(dataName: ProcessRequestDataName): String? = data[dataName]

    fun getDataValueOrEmpty(dataName: ProcessRequestDataName): String = data[dataName].orEmpty()

    fun getStakeholderValue(stakeholderType: ProcessStakeholderType): String =
        stakeholders[stakeholderType] ?: throw ServerException(
            "Missing process data!!"
        )
}

data class CreateNewProcessPayload(
    val userId: UUID,
    val publicId: UUID,
    val type: ProcessType,
    val description: String,
    val initialState: ProcessState,
    val requestState: ProcessState,
    val channel: Channel,
    val data: Map<ProcessRequestDataName, String> = mapOf(),
    val stakeholders: Map<ProcessStakeholderType, String> = mapOf(),
    val externalReference: String? = null,
)

data class MakeProcessRequestPayload(
    val userId: UUID,
    val processPublicId: UUID,
    val eventType: ProcessEvent,
    val requestType: ProcessRequestType,
    val channel: Channel,
    val state: ProcessState = ProcessState.PENDING,
    val data: Map<ProcessRequestDataName, String> = mapOf(),
    val stakeholders: Map<ProcessStakeholderType, String> = mapOf(),
)

data class ProcessTransitionDto(
    val id: Long,
    val event: ProcessEvent,
    val userId: UUID,
    val oldState: ProcessState,
    val newState: ProcessState,
    val timestamp: Instant
)