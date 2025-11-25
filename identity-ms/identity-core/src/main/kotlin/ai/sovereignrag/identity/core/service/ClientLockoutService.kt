package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import io.github.oshai.kotlinlogging.KotlinLogging
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

        clientRepository.findByClientId(clientId)?.let { client ->
            client.recordFailedAuth()
            clientRepository.save(client)

            log.warn {
                "Client $clientId failed auth attempts: ${client.failedAuthAttempts}/${OAuthRegisteredClient.MAX_FAILED_ATTEMPTS}" +
                    client.lockedUntil?.let { " - Client locked until: $it" }.orEmpty()
            }
        }
    }

    @Transactional
    fun handleSuccessfulClientAuth(clientId: String) {
        log.info { "Successful authentication for client: $clientId" }

        clientRepository.findByClientId(clientId)
            ?.takeIf { it.failedAuthAttempts > 0 }
            ?.let { client ->
                log.info { "Resetting failed authentication attempts for client: $clientId" }
                client.resetFailedAuthAttempts()
                clientRepository.save(client)
            }
    }

    @Transactional
    fun checkClientLockStatus(client: OAuthRegisteredClient) {
        client.checkAndUnlockIfExpired()
            .takeIf { it }
            ?.let {
                clientRepository.save(client)
                log.info { "Lockout expired for client: ${client.clientId}, client unlocked" }
                return
            }

        client.takeIf { it.isCurrentlyLocked() }?.let {
            throw ClientLockedException(
                clientId = it.clientId,
                lockedUntil = it.lockedUntil!!,
                failedAttempts = it.failedAuthAttempts
            )
        }
    }

    fun getRemainingLockoutMinutes(clientId: String): Long? =
        clientRepository.findByClientId(clientId)
            ?.lockedUntil
            ?.takeIf { Instant.now().isBefore(it) }
            ?.let { (it.epochSecond - Instant.now().epochSecond) / 60 }

    @Transactional
    fun unlockClient(clientId: String): Boolean =
        clientRepository.findByClientId(clientId)
            ?.takeIf { it.isCurrentlyLocked() || it.failedAuthAttempts > 0 }
            ?.let { client ->
                log.info { "Manually unlocking client: $clientId" }
                client.resetFailedAuthAttempts()
                clientRepository.save(client)
                true
            }
            ?: false
}
