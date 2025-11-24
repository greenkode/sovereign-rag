package ai.sovereignrag.accounting.service

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class TransactionExportLockService(
    hazelcastInstance: HazelcastInstance
) {

    private val lockMap: IMap<String, LockInfo> = hazelcastInstance.getMap("transaction-export-locks")

    companion object {
        private const val EXPORT_LOCK_KEY = "transaction:export:lock"
        private const val LOCK_TIMEOUT_HOURS = 2L
        private const val EXTEND_THRESHOLD_MINUTES = 15L
    }

    data class LockInfo(
        val processId: String,
        val timestamp: Long,
        val expiryTime: Long
    )

    fun acquireExportLock(processId: String): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val expiryTime = now + Duration.ofHours(LOCK_TIMEOUT_HOURS).toMillis()
            val lockInfo = LockInfo(processId, now, expiryTime)

            val existingLock = lockMap.putIfAbsent(
                EXPORT_LOCK_KEY,
                lockInfo,
                LOCK_TIMEOUT_HOURS,
                TimeUnit.HOURS
            )

            val acquired = existingLock == null || existingLock.expiryTime < now

            if (acquired && existingLock != null) {
                lockMap.set(EXPORT_LOCK_KEY, lockInfo, LOCK_TIMEOUT_HOURS, TimeUnit.HOURS)
            }

            if (acquired) {
                log.debug { "Acquired transaction export lock for process $processId" }
            } else {
                log.warn { "Failed to acquire transaction export lock for process $processId. Current lock: ${existingLock?.processId}" }
            }

            acquired
        } catch (e: Exception) {
            log.error(e) { "Error acquiring export lock for process $processId" }
            false
        }
    }

    fun releaseExportLock(processId: String) {
        try {
            val currentLock = lockMap.get(EXPORT_LOCK_KEY)

            if (currentLock?.processId == processId) {
                lockMap.delete(EXPORT_LOCK_KEY)
                log.debug { "Released transaction export lock for process $processId" }
            } else {
                log.warn { "Cannot release lock for process $processId. Current lock belongs to: ${currentLock?.processId}" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error releasing export lock for process $processId" }
        }
    }

    fun extendLockIfNeeded(processId: String) {
        try {
            val currentLock = lockMap.get(EXPORT_LOCK_KEY)

            if (currentLock?.processId == processId) {
                val now = System.currentTimeMillis()
                val remaining = (currentLock.expiryTime - now) / (60 * 1000)

                if (remaining <= EXTEND_THRESHOLD_MINUTES) {
                    val newExpiryTime = now + Duration.ofHours(LOCK_TIMEOUT_HOURS).toMillis()
                    val updatedLock = currentLock.copy(expiryTime = newExpiryTime)
                    lockMap.set(EXPORT_LOCK_KEY, updatedLock, LOCK_TIMEOUT_HOURS, TimeUnit.HOURS)
                    log.info { "Extended transaction export lock for process $processId" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error extending export lock for process $processId" }
        }
    }

    fun getCurrentLockOwner(): String? {
        return try {
            lockMap.get(EXPORT_LOCK_KEY)?.processId
        } catch (e: Exception) {
            log.error(e) { "Error getting current lock owner" }
            null
        }
    }
}