package ai.sovereignrag.identity.process.spi


import ai.sovereignrag.identity.commons.exception.ServerException
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.ProcessTransitionDto
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.process.domain.ProcessEntity
import ai.sovereignrag.identity.process.domain.ProcessRepository
import ai.sovereignrag.identity.process.domain.ProcessRequestEntity
import ai.sovereignrag.identity.process.domain.model.ProcessCreatedEvent
import ai.sovereignrag.identity.process.orchestrator.ProcessOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID


@Service
@Transactional(readOnly = true)
class ProcessService(
    private val processRepository: ProcessRepository,
    private val publisher: ApplicationEventPublisher,
    private val processOrchestrator: ProcessOrchestrator,
) : ProcessGateway {

    val log = KotlinLogging.logger {}

    @Transactional
    override fun createProcess(payload: CreateNewProcessPayload): ProcessDto {

        val process = ProcessEntity(
            payload.publicId,
            payload.type,
            payload.description,
            payload.initialState,
            payload.channel,
            Instant.now().plusSeconds(payload.type.timeInSeconds),
            payload.externalReference,
        )

        val request =
            ProcessRequestEntity(
                process,
                payload.userId,
                ProcessRequestType.CREATE_NEW_PROCESS,
                ProcessState.COMPLETE,
                payload.channel,
            )

        payload.stakeholders.map { request.addStakeholder(it.key, it.value) }

        payload.data.forEach { (name, value) -> request.addData(name, value) }

        process.addRequest(request)

        // Add initial transition to track process creation
        process.addTransition(ProcessState.INITIAL, payload.initialState, ProcessEvent.PROCESS_CREATED, payload.userId)

        // Save the process with its request and data
        val savedProcess = processRepository.save(process)

        val result = savedProcess.toDomain()

        publisher.publishEvent(ProcessCreatedEvent(result.publicId, result.type))

        return result.toDto()
    }

    override fun findPendingProcessByPublicId(id: UUID): ProcessDto? {
        return processRepository.findByPublicIdAndState(id, ProcessState.PENDING)?.takeIf { it.expiry.isAfter(Instant.now()) }?.toDomain()?.toDto()
    }

    override fun findPendingProcessByExternalReference(externalReference: String): ProcessDto? {
        return processRepository.findByExternalReferenceAndState(externalReference, ProcessState.PENDING)?.takeIf { it.expiry.isAfter(Instant.now()) }?.toDomain()
            ?.toDto()
    }

    override fun findPendingProcessByTypeAndExternalReference(
        type: ProcessType,
        externalReference: String
    ): ProcessDto? {
        return processRepository.findByExternalReferenceAndTypeAndState(externalReference, type, ProcessState.PENDING)?.takeIf { it.expiry.isAfter(Instant.now()) }
            ?.toDomain()?.toDto()
    }

    override fun findPendingProcessByTypesAndExternalReference(
        types: Set<ProcessType>,
        externalReference: String
    ): ProcessDto? {
        return processRepository.findByExternalReferenceAndTypeInAndState(
            externalReference,
            types,
            ProcessState.PENDING
        )?.takeIf { it.expiry.isAfter(Instant.now()) }?.toDomain()?.toDto()
    }

    @Transactional
    override fun makeRequest(payload: MakeProcessRequestPayload) {

        val process = processRepository.findByPublicId(payload.processPublicId)
            ?: throw ServerException("Unable to proceed with this process, please try again later")

        val request = ProcessRequestEntity(
            process,
            payload.userId,
            payload.requestType,
            payload.state,
            payload.channel
        )

        payload.stakeholders.forEach { request.addStakeholder(it.key, it.value) }

        payload.data.forEach { (name, value) -> request.addData(name, value) }

        process.addRequest(request)

        // Use new orchestrator instead of state machine - it will handle the save
        processOrchestrator.processEvent(process, payload.eventType, payload.userId)
    }

    override fun completeProcess(processId: UUID, requestId: Long) {

        updateProcessRequestState(processId, requestId, ProcessState.COMPLETE)

        updateProcessState(processId, ProcessState.COMPLETE)
    }

    override fun updateProcessRequestState(processId: UUID, requestId: Long, state: ProcessState) {
        processRepository.updateRequestStateIfProcessInState(
            processId = processId,
            requestId = requestId,
            newState = state,
            processState = ProcessState.PENDING
        )
    }

    fun updateProcessState(publicId: UUID, newState: ProcessState) {
        processRepository.updateStateIfInState(
            publicId = publicId,
            newState = newState,
            currentState = ProcessState.PENDING
        )
    }

    override fun failProcess(publicId: UUID) {
        updateProcessState(publicId, ProcessState.FAILED)
    }

    override fun expireProcess(id: UUID, internal: Boolean) {
        updateProcessState(id, ProcessState.EXPIRED)
    }

    fun processEvent(processId: UUID, event: ProcessEvent, userId: UUID) {
        processOrchestrator.processEvent(processId, event, userId)
    }

    override fun getProcessTransitions(processId: UUID): List<ProcessTransitionDto> {
        return processRepository.findTransitionsByProcessId(processId).map { transition ->
            ProcessTransitionDto(
                id = transition.id,
                event = transition.event,
                userId = transition.userId,
                oldState = transition.oldState,
                newState = transition.newState,
                timestamp = transition.createdAt
            )
        }.sortedBy { it.timestamp }
    }

    override fun findRecentPendingProcessesByTypeAndForUserId(
        processType: ProcessType,
        userId: UUID,
        since: Instant
    ): List<ProcessDto> {
        return processRepository.findRecentPendingProcessesByTypeAndForUserId(
            processType,
            ProcessStakeholderType.FOR_USER,
            userId.toString(),
            since,
            ProcessState.PENDING
        ).map { it.toDomain().toDto() }
    }

    override fun findLatestPendingProcessesByTypeAndForUserId(
        processType: ProcessType,
        userId: UUID
    ): ProcessDto? {
        return processRepository.findLatestPendingProcessByTypeAndForUserId(
            processType,
            ProcessStakeholderType.FOR_USER,
            userId.toString()
        )?.toDomain()?.toDto()
    }
}
