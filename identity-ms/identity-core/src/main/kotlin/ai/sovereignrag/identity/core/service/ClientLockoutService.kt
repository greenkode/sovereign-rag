package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import mu.KotlinLogging
import org.springframework.security.authentication.LockedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

class ClientLockedException(
    val clientId: String,
    val lockedUntil: Instant,
    val failedAttempts: Int
) : LockedException("Client locked due to $failedAttempts failed authentication attempts. Locked until: $lockedUntil")

@Service
class ClientLockoutService(
    private val clientRepository: OAuthRegisteredClientRepository
) {

    @Transactional
    fun handleFailedClientAuth(clientId: String) {
        log.warn { "Failed authentication attempt for client: $clientId" }
        
        val client = clientRepository.findByClientId(clientId)
        if (client != null) {
            client.recordFailedAuth()
            clientRepository.save(client)
            
            log.warn { 
                "Client $clientId failed auth attempts: ${client.failedAuthAttempts}/${OAuthRegisteredClient.MAX_FAILED_ATTEMPTS}" +
                if (client.isCurrentlyLocked()) " - Client locked until: ${client.lockedUntil}" else ""
            }
        }
    }

    @Transactional
    fun handleSuccessfulClientAuth(clientId: String) {
        log.info { "Successful authentication for client: $clientId" }
        
        val client = clientRepository.findByClientId(clientId)
        if (client != null && client.failedAuthAttempts > 0) {
            log.info { "Resetting failed authentication attempts for client: $clientId" }
            client.resetFailedAuthAttempts()
            clientRepository.save(client)
        }
    }

    @Transactional
    fun checkClientLockStatus(client: OAuthRegisteredClient) {
        // First check if lockout has expired and unlock if so
        if (client.checkAndUnlockIfExpired()) {
            clientRepository.save(client)
            log.info { "Lockout expired for client: ${client.clientId}, client unlocked" }
            return
        }

        // Check if client is currently locked
        if (client.isCurrentlyLocked()) {
            throw ClientLockedException(
                clientId = client.clientId,
                lockedUntil = client.lockedUntil!!,
                failedAttempts = client.failedAuthAttempts
            )
        }
    }

    fun getRemainingLockoutMinutes(clientId: String): Long? {
        val client = clientRepository.findByClientId(clientId) ?: return null
        
        if (client.lockedUntil == null) return null
        
        val now = Instant.now()
        return if (now.isBefore(client.lockedUntil)) {
            (client.lockedUntil!!.epochSecond - now.epochSecond) / 60
        } else {
            null
        }
    }

    @Transactional
    fun unlockClient(clientId: String): Boolean {
        val client = clientRepository.findByClientId(clientId) ?: return false
        
        if (client.isCurrentlyLocked() || client.failedAuthAttempts > 0) {
            log.info { "Manually unlocking client: $clientId" }
            client.resetFailedAuthAttempts()
            clientRepository.save(client)
            return true
        }
        
        return false
    }
}