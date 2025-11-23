package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import mu.KotlinLogging
import org.springframework.security.authentication.LockedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * Exception thrown when an account is locked due to too many failed login attempts
 */
class AccountLockedException(
    val username: String,
    val lockedUntil: Instant,
    val failedAttempts: Int
) : LockedException("Account locked due to $failedAttempts failed login attempts. Locked until: $lockedUntil")

@Service
class AccountLockoutService(
    private val userRepository: OAuthUserRepository
) {

    /**
     * Handles failed login attempt by incrementing counter and potentially locking account
     */
    @Transactional
    fun handleFailedLogin(username: String) {
        log.warn { "Failed login attempt for user: $username" }
        
        val user = userRepository.findByUsername(username)
        if (user != null) {
            user.recordFailedLogin()
            userRepository.save(user)
            
            log.warn { 
                "User $username failed login attempts: ${user.failedLoginAttempts}/${OAuthUser.MAX_FAILED_ATTEMPTS}" +
                if (user.isCurrentlyLocked()) " - Account locked until: ${user.lockedUntil}" else ""
            }
        }
    }

    /**
     * Handles successful login by resetting failed attempts counter
     */
    @Transactional
    fun handleSuccessfulLogin(username: String) {
        log.info { "Successful login for user: $username" }
        
        val user = userRepository.findByUsername(username)
        if (user != null && user.failedLoginAttempts > 0) {
            log.info { "Resetting failed login attempts for user: $username" }
            user.resetFailedLoginAttempts()
            userRepository.save(user)
        }
    }

    /**
     * Checks if user account is locked and throws appropriate exception
     */
    @Transactional
    fun checkAccountLockStatus(user: OAuthUser) {
        // First check if lockout has expired and unlock if so
        if (user.checkAndUnlockIfExpired()) {
            userRepository.save(user)
            log.info { "Lockout expired for user: ${user.username}, account unlocked" }
            return
        }

        // Check if account is currently locked
        if (user.isCurrentlyLocked()) {
            throw AccountLockedException(
                username = user.username,
                lockedUntil = user.lockedUntil!!,
                failedAttempts = user.failedLoginAttempts
            )
        }
    }

    /**
     * Gets remaining lockout time in minutes for a user
     */
    fun getRemainingLockoutMinutes(username: String): Long? {
        val user = userRepository.findByUsername(username) ?: return null
        
        if (user.lockedUntil == null) return null
        
        val now = Instant.now()
        return if (now.isBefore(user.lockedUntil)) {
            (user.lockedUntil!!.epochSecond - now.epochSecond) / 60
        } else {
            null
        }
    }

    /**
     * Manually unlocks a user account (for admin purposes)
     */
    @Transactional
    fun unlockAccount(username: String): Boolean {
        val user = userRepository.findByUsername(username) ?: return false
        
        if (user.isCurrentlyLocked() || user.failedLoginAttempts > 0) {
            log.info { "Manually unlocking account for user: $username" }
            user.resetFailedLoginAttempts()
            userRepository.save(user)
            return true
        }
        
        return false
    }
}