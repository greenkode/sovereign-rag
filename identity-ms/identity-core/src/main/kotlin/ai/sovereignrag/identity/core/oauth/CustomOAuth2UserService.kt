package ai.sovereignrag.identity.core.oauth

import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthProviderAccount
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.RegistrationSource
import ai.sovereignrag.identity.core.repository.OAuthProviderAccountRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.BusinessEmailValidationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CustomOAuth2UserService(
    private val userRepository: OAuthUserRepository,
    private val providerAccountRepository: OAuthProviderAccountRepository,
    private val businessEmailValidationService: BusinessEmailValidationService,
    private val passwordEncoder: PasswordEncoder,
    private val messageService: MessageService
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        log.info { "OAuth2 login attempt from provider: $registrationId" }

        val provider = parseProvider(registrationId)
        val providerUserId = extractProviderUserId(oauth2User, provider)
        val email = extractEmail(oauth2User, provider)
            ?: throw ClientException(messageService.getMessage("oauth.error.email_required"))

        businessEmailValidationService.validateBusinessEmail(email)

        val (user, _) = findOrCreateUser(provider, providerUserId, email, oauth2User)

        return OAuth2UserPrincipal(
            oauth2User = oauth2User,
            internalUser = user,
            provider = provider
        )
    }

    private fun parseProvider(registrationId: String): OAuthProvider =
        when (registrationId.lowercase()) {
            "google" -> OAuthProvider.GOOGLE
            "microsoft" -> OAuthProvider.MICROSOFT
            else -> throw ClientException(messageService.getMessage("oauth.error.unsupported_provider", registrationId))
        }

    private fun extractProviderUserId(oauth2User: OAuth2User, provider: OAuthProvider): String =
        when (provider) {
            OAuthProvider.GOOGLE -> oauth2User.getAttribute<String>("sub")
            OAuthProvider.MICROSOFT -> oauth2User.getAttribute<String>("sub") ?: oauth2User.getAttribute<String>("oid")
        } ?: throw ClientException(messageService.getMessage("oauth.error.missing_user_id"))

    private fun extractEmail(oauth2User: OAuth2User, provider: OAuthProvider): String? =
        when (provider) {
            OAuthProvider.GOOGLE -> oauth2User.getAttribute("email")
            OAuthProvider.MICROSOFT -> oauth2User.getAttribute("email")
                ?: oauth2User.getAttribute("preferred_username")
        }

    private fun findOrCreateUser(
        provider: OAuthProvider,
        providerUserId: String,
        email: String,
        oauth2User: OAuth2User
    ): Pair<OAuthUser, Boolean> {
        val existingProviderAccount = providerAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)

        existingProviderAccount?.let { account ->
            account.updateLastLogin()
            providerAccountRepository.save(account)
            log.info { "Existing OAuth user logged in: ${account.user.email} via $provider" }
            return Pair(account.user, false)
        }

        val existingUser = userRepository.findByEmail(email)

        existingUser?.let { user ->
            val providerAccount = OAuthProviderAccount(
                user = user,
                provider = provider,
                providerUserId = providerUserId,
                providerEmail = email
            )
            providerAccount.updateLastLogin()
            providerAccountRepository.save(providerAccount)
            log.info { "Linked $provider account to existing user: $email" }
            return Pair(user, false)
        }

        val newUser = createNewUser(provider, email, oauth2User)
        val providerAccount = OAuthProviderAccount(
            user = newUser,
            provider = provider,
            providerUserId = providerUserId,
            providerEmail = email
        )
        providerAccount.updateLastLogin()
        providerAccountRepository.save(providerAccount)

        log.info { "Created new user via $provider: $email" }
        return Pair(newUser, true)
    }

    private fun createNewUser(provider: OAuthProvider, email: String, oauth2User: OAuth2User): OAuthUser {
        val firstName = oauth2User.getAttribute<String>("given_name")
            ?: oauth2User.getAttribute<String>("name")?.split(" ")?.firstOrNull()
        val lastName = oauth2User.getAttribute<String>("family_name")
            ?: oauth2User.getAttribute<String>("name")?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() }

        val user = OAuthUser(
            username = email,
            password = passwordEncoder.encode(UUID.randomUUID().toString()),
            email = email
        ).apply {
            this.firstName = firstName
            this.lastName = lastName
            this.emailVerified = true
            this.enabled = true
            this.registrationSource = when (provider) {
                OAuthProvider.GOOGLE -> RegistrationSource.OAUTH_GOOGLE
                OAuthProvider.MICROSOFT -> RegistrationSource.OAUTH_MICROSOFT
            }
            this.authorities = mutableSetOf("ROLE_USER")
        }

        return userRepository.save(user)
    }
}
