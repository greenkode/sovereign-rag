package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
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
            ?: throw UsernameNotFoundException("User not found: $username")
                .also { log.warn { "User not found: $username" } }

        accountLockoutService.checkAccountLockStatus(oauthUser)

        log.info { "Found user: ${oauthUser.username}, enabled: ${oauthUser.enabled}, locked: ${oauthUser.isCurrentlyLocked()}, authorities: ${oauthUser.authorities}" }

        return CustomUserDetails(oauthUser)
            .also { log.info { "Created UserDetails for: ${it.username}" } }
    }
}
