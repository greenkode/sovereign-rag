package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import mu.KotlinLogging
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CustomUserDetailsService(
    private val userRepository: OAuthUserRepository,
    private val accountLockoutService: AccountLockoutService
) : UserDetailsService {
    
    override fun loadUserByUsername(username: String): UserDetails {
        log.info { "Loading user by username: $username" }
        
        val oauthUser = userRepository.findByUsername(username)
        if (oauthUser == null) {
            log.warn { "User not found: $username" }
            throw UsernameNotFoundException("User not found: $username")
        }
        
        // Check account lockout status before proceeding
        accountLockoutService.checkAccountLockStatus(oauthUser)
        
        log.info { "Found user: ${oauthUser.username}, enabled: ${oauthUser.enabled}, locked: ${oauthUser.isCurrentlyLocked()}, authorities: ${oauthUser.authorities}" }
        
        val userDetails = CustomUserDetails(oauthUser)
        
        log.info { "Created UserDetails for: ${userDetails.username}" }
        return userDetails
    }
}