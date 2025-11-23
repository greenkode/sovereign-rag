package ai.sovereignrag.identity.process.orchestrator


import ai.sovereignrag.identity.commons.exception.ServerException
import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.process.domain.ProcessEntity
import ai.sovereignrag.identity.process.domain.ProcessRepository
import ai.sovereignrag.identity.process.domain.model.ProcessStateChangedEvent
import ai.sovereignrag.identity.process.event.AsyncEventPublisher
import ai.sovereignrag.identity.process.strategy.ProcessStrategy
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ProcessOrchestrator(
    private val processRepository: ProcessRepository,
    private val applicationContext: ApplicationContext,
    private val asyncEventPublisher: AsyncEventPublisher
) {
    
    private val log = KotlinLogging.logger {}

    private val strategyCache = mutableMapOf<ProcessType, ProcessStrategy>()

    private val processConversionCache = mutableMapOf<Long, ProcessDto>()

    fun processEvent(processId: UUID, event: ProcessEvent, userId: UUID) {
        
        log.info { "Processing event $event for process $processId" }
        
        val process = processRepository.findByPublicId(processId)
            ?: throw ServerException("Process not found: $processId")
        
        processEventInternal(process, event, userId)
    }

    fun processEvent(process: ProcessEntity, event: ProcessEvent, userId: UUID) {
        log.info { "Processing event $event for process ${process.publicId}" }
        processEventInternal(process, event, userId)
    }
    
    private fun processEventInternal(process: ProcessEntity, event: ProcessEvent, userId: UUID) {
        val processDto = processConversionCache.getOrPut(process.id!!) {
            process.toDomain().toDto()
        }
        
        val strategy = findStrategyForProcess(processDto.type)
            ?: throw ServerException("No strategy found for process type: ${processDto.type}")
        
        val oldState = process.state
        
        try {

            val expectedNewState = strategy.calculateExpectedState(oldState, event)
            if (!strategy.isValidTransition(oldState, event, expectedNewState)) {
                throw ServerException("Invalid state transition: $oldState -> $event -> $expectedNewState")
            }

            val newState = strategy.processEvent(processDto, event)

            process.addTransition(oldState, newState, event, userId)

            if (oldState != newState) {
                process.updateState(newState)
            }

            processRepository.save(process)

            processConversionCache.remove(process.id)

            if (oldState != newState) {
                val stateChangeEvent = ProcessStateChangedEvent(
                    processId = process.publicId,
                    processType = processDto.type,
                    oldState = oldState,
                    newState = newState,
                    userId = userId
                )
                asyncEventPublisher.publishStateChangeEvent(stateChangeEvent)
                
                log.info { "Process ${process.publicId} state changed: $oldState -> $newState" }
            } else {
                log.info { "Process ${process.publicId} processed event $event without state change (remained in $oldState)" }
            }
            
        } catch (e: Exception) {
            log.error(e) { "Error processing event $event for process ${process.publicId}" }

            if (!isTerminalState(oldState)) {
                process.addTransition(oldState, ProcessState.FAILED, ProcessEvent.PROCESS_FAILED, userId)
                process.updateState(ProcessState.FAILED)
                processRepository.save(process)

                processConversionCache.remove(process.id)
                
                val failureEvent = ProcessStateChangedEvent(
                    processId = process.publicId,
                    processType = processDto.type,
                    oldState = oldState,
                    newState = ProcessState.FAILED,
                    userId = userId
                )
                asyncEventPublisher.publishStateChangeEvent(failureEvent)
                
                log.info { "Process ${process.publicId} failed: $oldState -> FAILED" }
            }
            
            throw e
        }
    }
    
    private fun findStrategyForProcess(processType: ProcessType): ProcessStrategy? {
        return strategyCache.getOrPut(processType) {
            val strategyBeanName = processType.strategyBeanName ?: return null
            try {
                applicationContext.getBean(strategyBeanName, ProcessStrategy::class.java)
            } catch (_: Exception) {
                log.error { "Failed to find strategy bean: $strategyBeanName for process type: $processType" }
                return null
            }
        }
    }
    
    private fun isTerminalState(state: ProcessState): Boolean {
        return state in setOf(ProcessState.COMPLETE, ProcessState.FAILED, ProcessState.EXPIRED, ProcessState.CANCELLED)
    }
    
}