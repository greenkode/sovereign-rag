package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthProviderAccount
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.RegistrationSource
import ai.sovereignrag.identity.core.fixtures.OAuthClientBuilder
import ai.sovereignrag.identity.core.fixtures.ProviderAccountBuilder
import ai.sovereignrag.identity.core.fixtures.UserBuilder
import ai.sovereignrag.identity.core.oauth.CustomOidcUserService
import ai.sovereignrag.identity.core.oauth.OAuth2UserPrincipal
import ai.sovereignrag.identity.core.organization.entity.Organization
import ai.sovereignrag.identity.core.organization.repository.OrganizationRepository
import ai.sovereignrag.identity.core.repository.OAuthProviderAccountRepository
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.BusinessEmailValidationService
import ai.sovereignrag.identity.core.service.OAuthClientConfigService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomOidcUserServiceTest {

    private val userRepository: OAuthUserRepository = mockk()
    private val providerAccountRepository: OAuthProviderAccountRepository = mockk()
    private val oauthClientRepository: OAuthRegisteredClientRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val oauthClientConfigService: OAuthClientConfigService = mockk()
    private val businessEmailValidationService: BusinessEmailValidationService = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val messageService: MessageService = mockk()

    private lateinit var service: TestableCustomOidcUserService

    @BeforeEach
    fun setup() {
        service = TestableCustomOidcUserService(
            userRepository,
            providerAccountRepository,
            oauthClientRepository,
            organizationRepository,
            oauthClientConfigService,
            businessEmailValidationService,
            passwordEncoder,
            messageService
        )
    }

    @Test
    fun `should return existing user when provider account exists`() {
        val existingUser = UserBuilder.oauthUser(
            email = "existing@testcompany.com",
            provider = RegistrationSource.OAUTH_GOOGLE
        )
        val providerAccount = ProviderAccountBuilder.googleAccount(
            user = existingUser,
            providerUserId = "google_user_123"
        )

        val oidcUser = createMockOidcUser("google_user_123", "existing@testcompany.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google_user_123") } returns providerAccount
        every { providerAccountRepository.save(any()) } answers { firstArg() }

        val result = service.loadUser(userRequest) as OAuth2UserPrincipal

        assertEquals(existingUser.email, result.internalUser.email)
        assertEquals(OAuthProvider.GOOGLE, result.provider)
        verify { providerAccountRepository.save(any()) }
    }

    @Test
    fun `should link provider to existing user found by email`() {
        val existingUser = UserBuilder.verifiedUser(
            email = "user@testcompany.com"
        )
        val userSlot = slot<OAuthUser>()
        val providerAccountSlot = slot<OAuthProviderAccount>()

        val oidcUser = createMockOidcUser("new_provider_123", "user@testcompany.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "new_provider_123") } returns null
        every { userRepository.findByEmail("user@testcompany.com") } returns existingUser
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        every { providerAccountRepository.save(capture(providerAccountSlot)) } answers { providerAccountSlot.captured }

        val result = service.loadUser(userRequest) as OAuth2UserPrincipal

        assertEquals(existingUser.email, result.internalUser.email)
        assertTrue(userSlot.captured.emailVerified)
        assertTrue(userSlot.captured.registrationComplete)
        assertEquals(OAuthProvider.GOOGLE, providerAccountSlot.captured.provider)
        assertEquals("new_provider_123", providerAccountSlot.captured.providerUserId)
    }

    @Test
    fun `should create new user and organization when domain does not exist`() {
        val userSlot = slot<OAuthUser>()
        val clientSlot = slot<OAuthRegisteredClient>()
        val providerAccountSlot = slot<OAuthProviderAccount>()

        val oidcUser = createMockOidcUser(
            providerUserId = "new_google_user",
            email = "newuser@newcompany.com",
            givenName = "New",
            familyName = "User"
        )
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        setupMocksForNewOrganization()
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "new_google_user") } returns null
        every { userRepository.findByEmail("newuser@newcompany.com") } returns null
        every { oauthClientRepository.findByDomain("newcompany.com") } returns null
        every { oauthClientRepository.save(capture(clientSlot)) } answers { clientSlot.captured }
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { providerAccountRepository.save(capture(providerAccountSlot)) } answers { providerAccountSlot.captured }

        val result = service.loadUser(userRequest) as OAuth2UserPrincipal

        assertEquals("newuser@newcompany.com", result.internalUser.email)
        assertEquals("New", userSlot.captured.firstName)
        assertEquals("User", userSlot.captured.lastName)
        assertTrue(userSlot.captured.emailVerified)
        assertTrue(userSlot.captured.registrationComplete)
        assertTrue(userSlot.captured.authorities.contains("ROLE_MERCHANT_SUPER_ADMIN"))
        assertEquals(RegistrationSource.OAUTH_GOOGLE, userSlot.captured.registrationSource)
        assertEquals("newcompany.com", clientSlot.captured.domain)
    }

    @Test
    fun `should throw exception when domain already exists and requires invitation`() {
        val existingOrganization = OAuthClientBuilder.default(
            id = UUID.randomUUID().toString(),
            domain = "existingcompany.com"
        )
        val adminUser = UserBuilder.adminUser(
            email = "admin@existingcompany.com",
            merchantId = UUID.fromString(existingOrganization.id)
        )

        val oidcUser = createMockOidcUser("new_user_123", "newuser@existingcompany.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "new_user_123") } returns null
        every { userRepository.findByEmail("newuser@existingcompany.com") } returns null
        every { oauthClientRepository.findByDomain("existingcompany.com") } returns existingOrganization
        every { userRepository.findSuperAdminsByMerchantId(UUID.fromString(existingOrganization.id)) } returns listOf(adminUser)
        every { messageService.getMessage(any<String>(), any()) } returns "Please contact admin@existingcompany.com for an invitation"

        val exception = assertThrows<OAuth2AuthenticationException> {
            service.loadUser(userRequest)
        }

        assertEquals("invitation_required", exception.error.errorCode)
    }

    @Test
    fun `should throw exception for unsupported provider`() {
        val oidcUser = createMockOidcUser("fb_user_123", "user@company.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("facebook")

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { messageService.getMessage(any<String>(), any()) } returns "Unsupported provider: facebook"

        val exception = assertThrows<OAuth2AuthenticationException> {
            service.loadUser(userRequest)
        }

        assertEquals("unsupported_provider", exception.error.errorCode)
    }

    @Test
    fun `should throw exception when email is missing`() {
        val oidcUser = createMockOidcUser("google_user_123", null)
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { messageService.getMessage("oauth.error.email_required") } returns "Email is required"

        val exception = assertThrows<OAuth2AuthenticationException> {
            service.loadUser(userRequest)
        }

        assertEquals("email_required", exception.error.errorCode)
    }

    @Test
    fun `should throw exception when user id is missing`() {
        val oidcUser = createMockOidcUser(null, "user@company.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { messageService.getMessage("oauth.error.missing_user_id") } returns "User ID is required"

        val exception = assertThrows<OAuth2AuthenticationException> {
            service.loadUser(userRequest)
        }

        assertEquals("missing_user_id", exception.error.errorCode)
    }

    @Test
    fun `should throw exception for blocked email domain`() {
        val oidcUser = createMockOidcUser("google_user_123", "user@gmail.com")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        every { businessEmailValidationService.validateBusinessEmail("user@gmail.com") } throws
                InvalidRequestException("Personal email domains are not allowed")

        val exception = assertThrows<InvalidRequestException> {
            service.loadUser(userRequest)
        }

        assertNotNull(exception)
        assertEquals("Personal email domains are not allowed", exception.message)
    }

    @Test
    fun `should normalize email to lowercase`() {
        val userSlot = slot<OAuthUser>()
        val clientSlot = slot<OAuthRegisteredClient>()
        val providerAccountSlot = slot<OAuthProviderAccount>()

        val oidcUser = createMockOidcUser("google_user_123", "User@Company.COM")
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        setupMocksForNewOrganization()
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google_user_123") } returns null
        every { userRepository.findByEmail("user@company.com") } returns null
        every { oauthClientRepository.findByDomain("company.com") } returns null
        every { oauthClientRepository.save(capture(clientSlot)) } answers { clientSlot.captured }
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { providerAccountRepository.save(capture(providerAccountSlot)) } answers { providerAccountSlot.captured }

        service.loadUser(userRequest)

        assertEquals("user@company.com", userSlot.captured.email)
    }

    @Test
    fun `should handle Microsoft provider correctly`() {
        val userSlot = slot<OAuthUser>()
        val clientSlot = slot<OAuthRegisteredClient>()
        val providerAccountSlot = slot<OAuthProviderAccount>()

        val oidcUser = createMockOidcUser(
            providerUserId = "ms_user_123",
            email = "user@mscompany.com",
            givenName = "MS",
            familyName = "User"
        )
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("microsoft")

        setupMocksForNewOrganization()
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.MICROSOFT, "ms_user_123") } returns null
        every { userRepository.findByEmail("user@mscompany.com") } returns null
        every { oauthClientRepository.findByDomain("mscompany.com") } returns null
        every { oauthClientRepository.save(capture(clientSlot)) } answers { clientSlot.captured }
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { providerAccountRepository.save(capture(providerAccountSlot)) } answers { providerAccountSlot.captured }

        val result = service.loadUser(userRequest) as OAuth2UserPrincipal

        assertEquals(OAuthProvider.MICROSOFT, result.provider)
        assertEquals(RegistrationSource.OAUTH_MICROSOFT, userSlot.captured.registrationSource)
    }

    @Test
    fun `should parse full name when given and family names are missing`() {
        val userSlot = slot<OAuthUser>()
        val clientSlot = slot<OAuthRegisteredClient>()
        val providerAccountSlot = slot<OAuthProviderAccount>()

        val oidcUser = createMockOidcUser(
            providerUserId = "google_user_123",
            email = "user@testcompany.com",
            givenName = null,
            familyName = null,
            fullName = "John Michael Doe"
        )
        service.setMockOidcUser(oidcUser)

        val userRequest = createOidcUserRequest("google")

        setupMocksForNewOrganization()
        every { providerAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google_user_123") } returns null
        every { userRepository.findByEmail("user@testcompany.com") } returns null
        every { oauthClientRepository.findByDomain("testcompany.com") } returns null
        every { oauthClientRepository.save(capture(clientSlot)) } answers { clientSlot.captured }
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { providerAccountRepository.save(capture(providerAccountSlot)) } answers { providerAccountSlot.captured }

        service.loadUser(userRequest)

        assertEquals("John", userSlot.captured.firstName)
        assertEquals("Michael Doe", userSlot.captured.lastName)
    }

    private fun setupMocksForNewOrganization() {
        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { passwordEncoder.encode(any()) } returns "encoded_password"
        every { organizationRepository.existsBySlug(any()) } returns false
        every { organizationRepository.save(any()) } answers { firstArg<Organization>() }
        every { oauthClientConfigService.getAuthenticationMethod("client_secret_basic") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getAuthenticationMethod("client_secret_post") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getGrantType("client_credentials") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getGrantType("refresh_token") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getScope("openid") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getScope("profile") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getScope("email") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getScope("read") } returns mockk(relaxed = true)
        every { oauthClientConfigService.getScope("write") } returns mockk(relaxed = true)
    }

    private fun createMockOidcUser(
        providerUserId: String?,
        email: String?,
        givenName: String? = null,
        familyName: String? = null,
        fullName: String? = null,
        picture: String? = null
    ): OidcUser {
        val oidcUser: OidcUser = mockk(relaxed = true)
        every { oidcUser.subject } returns providerUserId
        every { oidcUser.email } returns email
        every { oidcUser.givenName } returns givenName
        every { oidcUser.familyName } returns familyName
        every { oidcUser.fullName } returns fullName
        every { oidcUser.picture } returns picture
        return oidcUser
    }

    private fun createOidcUserRequest(registrationId: String): OidcUserRequest {
        val clientRegistration = ClientRegistration
            .withRegistrationId(registrationId)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("test-client-id")
            .redirectUri("http://localhost/callback")
            .authorizationUri("https://provider.com/auth")
            .tokenUri("https://provider.com/token")
            .userInfoUri("https://provider.com/userinfo")
            .userNameAttributeName("sub")
            .build()

        val idTokenClaims = mutableMapOf<String, Any>(
            "sub" to "test-subject",
            "iss" to "https://provider.com",
            "aud" to listOf("test-client-id")
        )

        val idToken = OidcIdToken(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            idTokenClaims
        )

        val accessToken = mockk<org.springframework.security.oauth2.core.OAuth2AccessToken>(relaxed = true)
        every { accessToken.tokenValue } returns "access-token-value"

        return OidcUserRequest(clientRegistration, accessToken, idToken)
    }

    private class TestableCustomOidcUserService(
        private val testUserRepository: OAuthUserRepository,
        private val testProviderAccountRepository: OAuthProviderAccountRepository,
        private val testOauthClientRepository: OAuthRegisteredClientRepository,
        private val testOrganizationRepository: OrganizationRepository,
        private val testOauthClientConfigService: OAuthClientConfigService,
        private val testBusinessEmailValidationService: BusinessEmailValidationService,
        private val testPasswordEncoder: PasswordEncoder,
        private val testMessageService: MessageService
    ) : CustomOidcUserService(
        testUserRepository,
        testProviderAccountRepository,
        testOauthClientRepository,
        testOrganizationRepository,
        testOauthClientConfigService,
        testBusinessEmailValidationService,
        testPasswordEncoder,
        testMessageService
    ) {
        private var mockOidcUser: OidcUser? = null

        fun setMockOidcUser(oidcUser: OidcUser) {
            this.mockOidcUser = oidcUser
        }

        override fun loadUser(userRequest: OidcUserRequest): OidcUser {
            val oidcUser = mockOidcUser ?: throw IllegalStateException("Mock OidcUser not set")
            return processOidcUser(userRequest, oidcUser)
        }

        private fun processOidcUser(userRequest: OidcUserRequest, oidcUser: OidcUser): OidcUser {
            val registrationId = userRequest.clientRegistration.registrationId

            val providerUserId = oidcUser.subject
                ?: throw oauthErrorInternal("missing_user_id", testMessageService.getMessage("oauth.error.missing_user_id"))
            val email = oidcUser.email?.lowercase()
                ?: throw oauthErrorInternal("email_required", testMessageService.getMessage("oauth.error.email_required"))

            val provider = parseProviderInternal(registrationId)

            testBusinessEmailValidationService.validateBusinessEmail(email)

            val user = findUserByProviderAccountInternal(provider, providerUserId)
                ?: findUserByEmailAndLinkProviderInternal(email, provider, providerUserId)
                ?: resolveUserForDomainInternal(email, provider, providerUserId, oidcUser)

            return OAuth2UserPrincipal(
                oauth2User = oidcUser,
                internalUser = user,
                provider = provider
            )
        }

        private fun oauthErrorInternal(errorCode: String, description: String): OAuth2AuthenticationException =
            OAuth2AuthenticationException(
                org.springframework.security.oauth2.core.OAuth2Error(errorCode, description, null),
                description
            )

        private fun parseProviderInternal(registrationId: String): OAuthProvider =
            when (registrationId.lowercase()) {
                "google" -> OAuthProvider.GOOGLE
                "microsoft" -> OAuthProvider.MICROSOFT
                else -> throw oauthErrorInternal("unsupported_provider", testMessageService.getMessage("oauth.error.unsupported_provider", registrationId))
            }

        private fun findUserByProviderAccountInternal(provider: OAuthProvider, providerUserId: String): OAuthUser? {
            return testProviderAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                ?.also { account ->
                    account.updateLastLogin()
                    testProviderAccountRepository.save(account)
                }?.user
        }

        private fun findUserByEmailAndLinkProviderInternal(email: String, provider: OAuthProvider, providerUserId: String): OAuthUser? {
            return testUserRepository.findByEmail(email)?.also { user ->
                user.emailVerified = true
                user.registrationComplete = true
                testUserRepository.save(user)

                val providerAccount = OAuthProviderAccount(
                    user = user,
                    provider = provider,
                    providerUserId = providerUserId,
                    providerEmail = email
                )
                providerAccount.updateLastLogin()
                testProviderAccountRepository.save(providerAccount)
            }
        }

        private fun resolveUserForDomainInternal(
            email: String,
            provider: OAuthProvider,
            providerUserId: String,
            oidcUser: OidcUser
        ): OAuthUser {
            val domain = email.substringAfter("@")

            return testOauthClientRepository.findByDomain(domain)?.let { client ->
                val superAdminEmail = findSuperAdminEmailInternal(client.id)
                throw oauthErrorInternal(
                    "invitation_required",
                    testMessageService.getMessage("oauth.error.domain_exists", superAdminEmail)
                )
            } ?: run {
                val (organizationId, oauthClientId) = createOAuthClientInternal(domain, email)
                createUserWithProviderAccountInternal(provider, providerUserId, email, oidcUser, organizationId, oauthClientId)
            }
        }

        private fun findSuperAdminEmailInternal(merchantId: UUID): String =
            testUserRepository.findSuperAdminsByMerchantId(merchantId)
                .firstOrNull()?.email
                ?: testMessageService.getMessage("oauth.error.admin_not_found")

        private fun createOAuthClientInternal(domain: String, adminEmail: String): Pair<UUID, UUID> {
            val slug = domain.substringBefore(".")
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')

            val organization = Organization(
                name = "-",
                slug = slug
            )
            val savedOrg = testOrganizationRepository.save(organization)

            val sandboxSecret = "test_sandbox_secret"
            val productionSecret = "test_production_secret"

            val authMethodBasic = testOauthClientConfigService.getAuthenticationMethod("client_secret_basic")
            val authMethodPost = testOauthClientConfigService.getAuthenticationMethod("client_secret_post")
            val grantTypeCredentials = testOauthClientConfigService.getGrantType("client_credentials")
            val grantTypeRefresh = testOauthClientConfigService.getGrantType("refresh_token")
            val scopeOpenid = testOauthClientConfigService.getScope("openid")
            val scopeProfile = testOauthClientConfigService.getScope("profile")
            val scopeEmail = testOauthClientConfigService.getScope("email")
            val scopeRead = testOauthClientConfigService.getScope("read")
            val scopeWrite = testOauthClientConfigService.getScope("write")

            val oauthClient = OAuthRegisteredClient().apply {
                id = savedOrg.id.toString()
                clientId = UUID.randomUUID().toString()
                clientName = "-"
                clientIdIssuedAt = Instant.now()
                clientSecret = testPasswordEncoder.encode(sandboxSecret)
                sandboxClientSecret = testPasswordEncoder.encode(sandboxSecret)
                productionClientSecret = testPasswordEncoder.encode(productionSecret)
                this.domain = domain
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
            }

            testOauthClientRepository.save(oauthClient)
            return savedOrg.id to UUID.fromString(oauthClient.id)
        }

        private fun createUserWithProviderAccountInternal(
            provider: OAuthProvider,
            providerUserId: String,
            email: String,
            oidcUser: OidcUser,
            organizationId: UUID,
            oauthClientId: UUID
        ): OAuthUser {
            val firstName = oidcUser.givenName ?: oidcUser.fullName?.split(" ")?.firstOrNull()
            val lastName = oidcUser.familyName ?: oidcUser.fullName?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() }
            val pictureUrl = oidcUser.picture

            val user = OAuthUser(
                username = email,
                password = testPasswordEncoder.encode(UUID.randomUUID().toString()),
                email = email
            ).apply {
                this.firstName = firstName
                this.lastName = lastName
                this.pictureUrl = pictureUrl
                this.emailVerified = true
                this.registrationComplete = true
                this.enabled = true
                this.organizationId = organizationId
                this.merchantId = oauthClientId
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

            val savedUser = testUserRepository.save(user)

            val providerAccount = OAuthProviderAccount(
                user = savedUser,
                provider = provider,
                providerUserId = providerUserId,
                providerEmail = email
            )
            providerAccount.updateLastLogin()
            testProviderAccountRepository.save(providerAccount)

            return savedUser
        }
    }
}
