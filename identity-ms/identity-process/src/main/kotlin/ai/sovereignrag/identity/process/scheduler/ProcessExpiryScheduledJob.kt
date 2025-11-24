package ai.sovereignrag.identity.process.scheduler

import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.process.domain.ProcessRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class ProcessExpiryScheduledJob(
    private val processRepository: ProcessRepository
) {
    
    private val log = KotlinLogging.logger {}
    
    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun expireProcesses() {
        val currentTime = Instant.now()
        
        val expiredCount = processRepository.bulkExpireProcesses(
            currentTime = currentTime,
            expiredState = ProcessState.EXPIRED,
            activeStates = setOf(ProcessState.PENDING)
        )
        
        if (expiredCount > 0) {
            log.info { "Expired $expiredCount processes at $currentTime" }
        }
    }
}