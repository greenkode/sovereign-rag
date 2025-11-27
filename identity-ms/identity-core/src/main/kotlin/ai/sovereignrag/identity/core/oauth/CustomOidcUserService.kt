package ai.sovereignrag.identity.core.oauth

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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CustomOidcUserService(
    private val userRepository: OAuthUserRepository,
    private val providerAccountRepository: OAuthProviderAccountRepository,
    private val businessEmailValidationService: BusinessEmailValidationService,
    private val passwordEncoder: PasswordEncoder,
    private val messageService: MessageService
) : OidcUserService() {

    @Transactional
    override fun loadUser(userRequest: OidcUserRequest): OidcUser =
        runCatching {
            val oidcUser = super.loadUser(userRequest)
            val registrationId = userRequest.clientRegistration.registrationId

            log.info { "OIDC login attempt from provider: $registrationId" }

            val provider = parseProvider(registrationId)
            val providerUserId = oidcUser.subject
                ?: throw oauthError("missing_user_id", messageService.getMessage("oauth.error.missing_user_id"))
            val email = oidcUser.email
                ?: throw oauthError("email_required", messageService.getMessage("oauth.error.email_required"))

            businessEmailValidationService.validateBusinessEmail(email)

            val user = findUserByProviderAccount(provider, providerUserId)
                ?: findUserByEmailAndLinkProvider(email, provider, providerUserId)
                ?: createUserWithProviderAccount(provider, providerUserId, email, oidcUser)

            OAuth2UserPrincipal(
                oauth2User = oidcUser,
                internalUser = user,
                provider = provider
            )
        }.getOrElse { e ->
            log.error(e) { "OIDC authentication failed: ${e.message}" }
            throw (e as? OAuth2AuthenticationException)
                ?: oauthError("authentication_failed", e.message ?: "Authentication failed")
        }

    private fun oauthError(errorCode: String, description: String): OAuth2AuthenticationException =
        OAuth2AuthenticationException(OAuth2Error(errorCode, description, null), description)

    private fun parseProvider(registrationId: String): OAuthProvider =
        when (registrationId.lowercase()) {
            "google" -> OAuthProvider.GOOGLE
            "microsoft" -> OAuthProvider.MICROSOFT
            else -> throw oauthError("unsupported_provider", messageService.getMessage("oauth.error.unsupported_provider", registrationId))
        }

    private fun findUserByProviderAccount(provider: OAuthProvider, providerUserId: String): OAuthUser? {
        return providerAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
            ?.also { account ->
                account.updateLastLogin()
                providerAccountRepository.save(account)
                log.info { "Existing OIDC user logged in: ${account.user.email} via $provider" }
            }?.user
    }

    private fun findUserByEmailAndLinkProvider(email: String, provider: OAuthProvider, providerUserId: String): OAuthUser? {
        return userRepository.findByEmail(email)?.also { user ->
            val providerAccount = OAuthProviderAccount(
                user = user,
                provider = provider,
                providerUserId = providerUserId,
                providerEmail = email
            )
            providerAccount.updateLastLogin()
            providerAccountRepository.save(providerAccount)
            log.info { "Linked $provider account to existing user: $email" }
        }
    }

    private fun createUserWithProviderAccount(
        provider: OAuthProvider,
        providerUserId: String,
        email: String,
        oidcUser: OidcUser
    ): OAuthUser {
        val firstName = oidcUser.givenName ?: oidcUser.fullName?.split(" ")?.firstOrNull()
        val lastName = oidcUser.familyName ?: oidcUser.fullName?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() }

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

        val savedUser = userRepository.save(user)

        val providerAccount = OAuthProviderAccount(
            user = savedUser,
            provider = provider,
            providerUserId = providerUserId,
            providerEmail = email
        )
        providerAccount.updateLastLogin()
        providerAccountRepository.save(providerAccount)

        log.info { "Created new user via $provider OIDC: $email" }
        return savedUser
    }
}
