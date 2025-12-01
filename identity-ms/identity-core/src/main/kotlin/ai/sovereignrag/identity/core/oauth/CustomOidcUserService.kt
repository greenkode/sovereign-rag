package ai.sovereignrag.identity.core.oauth

import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthClientSettingName
import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthProviderAccount
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthTokenSettingName
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.entity.RegistrationSource
import ai.sovereignrag.identity.core.entity.TrustLevel
import ai.sovereignrag.identity.core.entity.UserType
import ai.sovereignrag.identity.core.repository.OAuthProviderAccountRepository
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.BusinessEmailValidationService
import ai.sovereignrag.identity.core.service.OAuthClientConfigService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CustomOidcUserService(
    private val userRepository: OAuthUserRepository,
    private val providerAccountRepository: OAuthProviderAccountRepository,
    private val oauthClientRepository: OAuthRegisteredClientRepository,
    private val oauthClientConfigService: OAuthClientConfigService,
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
            val email = oidcUser.email?.lowercase()
                ?: throw oauthError("email_required", messageService.getMessage("oauth.error.email_required"))

            businessEmailValidationService.validateBusinessEmail(email)

            val user = findUserByProviderAccount(provider, providerUserId)
                ?: findUserByEmailAndLinkProvider(email, provider, providerUserId)
                ?: resolveUserForDomain(email, provider, providerUserId, oidcUser)

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

    private fun resolveUserForDomain(
        email: String,
        provider: OAuthProvider,
        providerUserId: String,
        oidcUser: OidcUser
    ): OAuthUser {
        val domain = email.substringAfter("@")

        return oauthClientRepository.findByDomain(domain)?.let { client ->
            val superAdminEmail = findSuperAdminEmail(UUID.fromString(client.id))
            throw oauthError(
                "invitation_required",
                messageService.getMessage("oauth.error.domain_exists", superAdminEmail)
            )
        } ?: run {
            val merchantId = createOAuthClient(domain, email)
            createUserWithProviderAccount(provider, providerUserId, email, oidcUser, merchantId)
        }
    }

    private fun findSuperAdminEmail(merchantId: UUID): String =
        userRepository.findSuperAdminsByMerchantId(merchantId)
            .firstOrNull()?.email
            ?: messageService.getMessage("oauth.error.admin_not_found")

    private fun createOAuthClient(domain: String, adminEmail: String): UUID {
        val name = "-"
        val organizationId = UUID.randomUUID()

        val sandboxSecret = RandomStringUtils.secure().nextAlphanumeric(30)
        val productionSecret = RandomStringUtils.secure().nextAlphanumeric(30)

        val authMethodBasic = oauthClientConfigService.getAuthenticationMethod("client_secret_basic")
        val authMethodPost = oauthClientConfigService.getAuthenticationMethod("client_secret_post")
        val grantTypeCredentials = oauthClientConfigService.getGrantType("client_credentials")
        val grantTypeRefresh = oauthClientConfigService.getGrantType("refresh_token")
        val scopeOpenid = oauthClientConfigService.getScope("openid")
        val scopeProfile = oauthClientConfigService.getScope("profile")
        val scopeEmail = oauthClientConfigService.getScope("email")
        val scopeRead = oauthClientConfigService.getScope("read")
        val scopeWrite = oauthClientConfigService.getScope("write")

        val oauthClient = OAuthRegisteredClient().apply {
            id = organizationId.toString()
            clientId = UUID.randomUUID().toString()
            clientName = name
            clientIdIssuedAt = Instant.now()
            clientSecret = passwordEncoder.encode(sandboxSecret)
            sandboxClientSecret = passwordEncoder.encode(sandboxSecret)
            productionClientSecret = passwordEncoder.encode(productionSecret)
            this.domain = domain
            this.status = OrganizationStatus.PENDING
            failedAuthAttempts = 0

            addAuthenticationMethod(authMethodBasic)
            addAuthenticationMethod(authMethodPost)
            addGrantType(grantTypeCredentials)
            addGrantType(grantTypeRefresh)
            addScope(scopeOpenid)
            addScope(scopeProfile)
            addScope(scopeEmail)
            addScope(scopeRead)
            addScope(scopeWrite)
            addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
            addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
            addSetting(OAuthClientSettingName.EMAIL, adminEmail)
            addSetting(OAuthClientSettingName.SETUP_COMPLETED, "false")
            addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
            addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
            addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
        }

        oauthClientRepository.save(oauthClient)
        log.info { "Created OAuth client via OIDC for organization: $name with clientId: ${oauthClient.clientId}, domain: $domain" }

        return organizationId
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
            user.emailVerified = true
            user.registrationComplete = true
            userRepository.save(user)

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
        oidcUser: OidcUser,
        merchantId: UUID
    ): OAuthUser {
        val firstName = oidcUser.givenName ?: oidcUser.fullName?.split(" ")?.firstOrNull()
        val lastName = oidcUser.familyName ?: oidcUser.fullName?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() }
        val pictureUrl = oidcUser.picture

        val user = OAuthUser(
            username = email,
            password = passwordEncoder.encode(UUID.randomUUID().toString()),
            email = email
        ).apply {
            this.firstName = firstName
            this.lastName = lastName
            this.pictureUrl = pictureUrl
            this.emailVerified = true
            this.registrationComplete = true
            this.enabled = true
            this.merchantId = merchantId
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_THREE
            this.registrationSource = when (provider) {
                OAuthProvider.GOOGLE -> RegistrationSource.OAUTH_GOOGLE
                OAuthProvider.MICROSOFT -> RegistrationSource.OAUTH_MICROSOFT
            }
            this.authorities = mutableSetOf(
                "ROLE_MERCHANT_ADMIN",
                "ROLE_MERCHANT_SUPER_ADMIN",
                "ROLE_MERCHANT_USER"
            )
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

        log.info { "Created new super admin user via $provider OIDC: $email for merchant: $merchantId" }
        return savedUser
    }
}
