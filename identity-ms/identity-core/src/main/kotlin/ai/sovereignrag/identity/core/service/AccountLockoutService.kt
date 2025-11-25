package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.authentication.LockedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

class AccountLockedException(
    val username: String,
    val lockedUntil: Instant,
    val failedAttempts: Int
) : LockedException("Account locked due to $failedAttempts failed login attempts. Locked until: $lockedUntil")

@Service
class AccountLockoutService(
    private val userRepository: OAuthUserRepository
) {

    @Transactional
    fun handleFailedLogin(username: String) {
        log.warn { "Failed login attempt for user: $username" }

        userRepository.findByUsername(username)?.let { user ->
            user.recordFailedLogin()
            userRepository.save(user)

            log.warn {
                "User $username failed login attempts: ${user.failedLoginAttempts}/${OAuthUser.MAX_FAILED_ATTEMPTS}" +
                    user.lockedUntil?.let { " - Account locked until: $it" }.orEmpty()
            }
        }
    }

    @Transactional
    fun handleSuccessfulLogin(username: String) {
        log.info { "Successful login for user: $username" }

        userRepository.findByUsername(username)
            ?.takeIf { it.failedLoginAttempts > 0 }
            ?.let { user ->
                log.info { "Resetting failed login attempts for user: $username" }
                user.resetFailedLoginAttempts()
                userRepository.save(user)
            }
    }

    @Transactional
    fun checkAccountLockStatus(user: OAuthUser) {
        user.checkAndUnlockIfExpired()
            .takeIf { it }
            ?.let {
                userRepository.save(user)
                log.info { "Lockout expired for user: ${user.username}, account unlocked" }
                return
            }

        user.takeIf { it.isCurrentlyLocked() }?.let {
            throw AccountLockedException(
                username = it.username,
                lockedUntil = it.lockedUntil!!,
                failedAttempts = it.failedLoginAttempts
            )
        }
    }

    fun getRemainingLockoutMinutes(username: String): Long? =
        userRepository.findByUsername(username)
            ?.lockedUntil
            ?.takeIf { Instant.now().isBefore(it) }
            ?.let { (it.epochSecond - Instant.now().epochSecond) / 60 }

    @Transactional
    fun unlockAccount(username: String): Boolean =
        userRepository.findByUsername(username)
            ?.takeIf { it.isCurrentlyLocked() || it.failedLoginAttempts > 0 }
            ?.let { user ->
                log.info { "Manually unlocking account for user: $username" }
                user.resetFailedLoginAttempts()
                userRepository.save(user)
                true
            }
            ?: false
}
