package ai.sovereignrag.identity.commons.process

import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import java.time.Instant
import java.util.UUID

interface ProcessGateway {

    fun createProcess(payload: CreateNewProcessPayload): ProcessDto

    fun findPendingProcessByPublicId(id: UUID): ProcessDto?

    fun findPendingProcessByTypeAndExternalReference(type: ProcessType, externalReference: String): ProcessDto?

    fun findPendingProcessByExternalReference(externalReference: String): ProcessDto?

    fun findPendingProcessByTypesAndExternalReference(types: Set<ProcessType>, externalReference: String): ProcessDto?

    fun makeRequest(payload: MakeProcessRequestPayload)

    fun completeProcess(processId: UUID, requestId: Long)

    fun updateProcessRequestState(processId: UUID, requestId: Long, state: ProcessState)

    fun failProcess(publicId: UUID)

    fun expireProcess(id: UUID, internal: Boolean)

    fun getProcessTransitions(processId: UUID): List<ProcessTransitionDto>

    fun findRecentPendingProcessesByTypeAndForUserId(processType: ProcessType, userId: UUID, since: Instant): List<ProcessDto>

    fun findLatestPendingProcessesByTypeAndForUserId(processType: ProcessType, userId: UUID): ProcessDto?
}