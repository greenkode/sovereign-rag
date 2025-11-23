package ai.sovereignrag.process.spi


import ai.sovereignrag.commons.exception.ProcessServiceException
import ai.sovereignrag.commons.performance.LogExecutionTime
import ai.sovereignrag.commons.process.CreateNewProcessPayload
import ai.sovereignrag.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.ProcessTransitionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.process.domain.ProcessEntity
import ai.sovereignrag.process.domain.ProcessRepository
import ai.sovereignrag.process.domain.ProcessRequestEntity
import ai.sovereignrag.process.domain.model.ProcessCreatedEvent
import ai.sovereignrag.process.domain.model.ProcessBasicInfo
import ai.sovereignrag.process.orchestrator.ProcessOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID


@Service
@Transactional
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

    override fun findByPublicId(internalReference: UUID): ProcessDto? {
        return processRepository.findByPublicId(internalReference)?.toDomain()?.toDto()
    }

    @LogExecutionTime
    fun findBasicInfoByPublicId(publicId: UUID): ProcessBasicInfo? {
        return processRepository.findBasicInfoByPublicId(publicId)
    }

    override fun findPendingProcessByPublicId(id: UUID): ProcessDto? {
        return processRepository.findByPublicIdAndState(id, ProcessState.PENDING)?.toDomain()?.toDto()
    }

    override fun findPendingProcessByExternalReference(externalReference: String): ProcessDto? {
        return processRepository.findByExternalReferenceAndState(externalReference, ProcessState.PENDING)?.toDomain()
            ?.toDto()
    }

    override fun findPendingProcessByType(type: ProcessType): ProcessDto? {
        val processes = processRepository.findByTypeAndState(type, ProcessState.PENDING)
        return processes.firstOrNull()?.toDomain()?.toDto()
    }

    override fun findPendingProcessesByType(type: ProcessType, limit: Int): List<ProcessDto> {
        val processes = processRepository.findByTypeAndState(type, ProcessState.PENDING)
        return processes.take(limit).map { it.toDomain().toDto() }
    }

    override fun hasPendingProcessOfType(type: ProcessType): Boolean {
        return processRepository.existsByTypeAndState(type, ProcessState.PENDING)
    }

    override fun findPendingProcessByTypeAndExternalReference(
        type: ProcessType,
        externalReference: String
    ): ProcessDto? {
        return processRepository.findByExternalReferenceAndTypeAndState(externalReference, type, ProcessState.PENDING)
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
        )?.toDomain()?.toDto()
    }

    @Transactional
    @LogExecutionTime
    override fun makeRequest(payload: MakeProcessRequestPayload) {

        val process = processRepository.findByPublicId(payload.processPublicId)
            ?: throw ProcessServiceException("{we.re.unable.to.complete.this.transfer.please.try.again.later}")

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

    @Transactional
    override fun completeProcess(processId: UUID, requestId: Long) {

        updateProcessRequestState(processId, requestId, ProcessState.COMPLETE)

        updateProcessState(processId, ProcessState.COMPLETE)
    }

    @Transactional
    override fun updateProcessRequestState(processId: UUID, requestId: Long, state: ProcessState) {
        processRepository.updateRequestStateIfProcessInState(
            processId = processId,
            requestId = requestId,
            newState = state,
            processState = ProcessState.PENDING
        )
    }

    @Transactional
    fun updateProcessState(publicId: UUID, newState: ProcessState) {
        processRepository.updateStateIfInState(
            publicId = publicId,
            newState = newState,
            currentState = ProcessState.PENDING
        )
    }

    @Transactional
    override fun failProcess(publicId: UUID) {
        updateProcessState(publicId, ProcessState.FAILED)
    }

    @Transactional
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
                timestamp = transition.createdDate
            )
        }.sortedBy { it.timestamp }
    }
    
    override fun findProcessesByType(type: ProcessType, pageable: Pageable): Page<ProcessDto> {
        return processRepository.findByType(type, pageable).map { it.toDomain().toDto() }
    }

    override fun findProcessByExternalReference(merchantReference: String): ProcessDto? {
        return processRepository.findByExternalReference(merchantReference)?.toDomain()?.toDto()
    }

    override fun findRecentProcessesByTypeAndForUserId(
        processType: ProcessType,
        userId: UUID,
        since: Instant
    ): List<ProcessDto> {
        return processRepository.findRecentProcessesByTypeAndForUserId(
            processType,
            ProcessStakeholderType.FOR_USER,
            userId.toString(),
            since
        ).map { it.toDomain().toDto() }
    }

    override fun findActivePendingProcessForTypeAndUser(
        type: ProcessType,
        userId: UUID,
        inactivityThreshold: Instant
    ): ProcessDto? {
        return processRepository.findActivePendingProcessForTypeAndUser(
            type,
            userId.toString(),
            ProcessStakeholderType.FOR_USER,
            inactivityThreshold
        )?.toDomain()?.toDto()
    }
}
