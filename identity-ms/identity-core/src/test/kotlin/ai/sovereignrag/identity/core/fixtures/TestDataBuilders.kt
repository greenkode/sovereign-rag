package ai.sovereignrag.identity.core.fixtures

import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessRequestDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.entity.OAuthAuthenticationMethod
import ai.sovereignrag.identity.core.entity.OAuthGrantType
import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthProviderAccount
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthScope
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.entity.RegistrationSource
import ai.sovereignrag.identity.core.entity.TrustLevel
import ai.sovereignrag.identity.core.entity.UserType
import ai.sovereignrag.identity.core.registration.command.RegisterUserCommand
import ai.sovereignrag.identity.core.registration.command.ResendVerificationCommand
import ai.sovereignrag.identity.core.registration.command.VerifyEmailCommand
import java.time.Instant
import java.util.UUID

object UserBuilder {
    fun default(
        id: UUID? = UUID.randomUUID(),
        email: String = "test@example.com",
        username: String = email,
        password: String = "encoded_password",
        firstName: String? = "Test",
        lastName: String? = "User",
        merchantId: UUID? = UUID.randomUUID(),
        emailVerified: Boolean = false,
        registrationComplete: Boolean = false,
        enabled: Boolean = true,
        userType: UserType = UserType.BUSINESS,
        trustLevel: TrustLevel = TrustLevel.TIER_THREE,
        registrationSource: RegistrationSource = RegistrationSource.SELF_REGISTRATION,
        authorities: MutableSet<String> = mutableSetOf("ROLE_USER")
    ): OAuthUser = OAuthUser().apply {
        this.id = id
        this.email = email
        this.username = username
        this.password = password
        this.firstName = firstName
        this.lastName = lastName
        this.merchantId = merchantId
        this.emailVerified = emailVerified
        this.registrationComplete = registrationComplete
        this.enabled = enabled
        this.userType = userType
        this.trustLevel = trustLevel
        this.registrationSource = registrationSource
        this.authorities = authorities
    }

    fun verifiedUser(
        id: UUID? = UUID.randomUUID(),
        email: String = "verified@example.com",
        merchantId: UUID? = UUID.randomUUID()
    ): OAuthUser = default(
        id = id,
        email = email,
        merchantId = merchantId,
        emailVerified = true,
        registrationComplete = true
    )

    fun unverifiedUser(
        id: UUID? = UUID.randomUUID(),
        email: String = "unverified@example.com",
        merchantId: UUID? = UUID.randomUUID()
    ): OAuthUser = default(
        id = id,
        email = email,
        merchantId = merchantId,
        emailVerified = false,
        registrationComplete = false
    )

    fun adminUser(
        id: UUID? = UUID.randomUUID(),
        email: String = "admin@example.com",
        merchantId: UUID? = UUID.randomUUID()
    ): OAuthUser = verifiedUser(
        id = id,
        email = email,
        merchantId = merchantId
    ).apply {
        authorities = mutableSetOf(
            "ROLE_USER",
            "ROLE_ADMIN",
            "ROLE_SUPER_ADMIN",
            "ROLE_MERCHANT_ADMIN",
            "ROLE_MERCHANT_SUPER_ADMIN",
            "ROLE_MERCHANT_USER"
        )
    }

    fun oauthUser(
        id: UUID? = UUID.randomUUID(),
        email: String = "oauth@example.com",
        merchantId: UUID? = UUID.randomUUID(),
        provider: RegistrationSource = RegistrationSource.OAUTH_GOOGLE
    ): OAuthUser = verifiedUser(
        id = id,
        email = email,
        merchantId = merchantId
    ).apply {
        registrationSource = provider
    }
}

object OAuthClientBuilder {
    fun default(
        id: String = UUID.randomUUID().toString(),
        clientId: String = UUID.randomUUID().toString(),
        clientName: String = "Test Organization",
        domain: String = "example.com",
        status: OrganizationStatus = OrganizationStatus.ACTIVE,
        clientSecret: String? = "encoded_secret",
        sandboxClientSecret: String? = "encoded_sandbox_secret",
        productionClientSecret: String? = "encoded_production_secret",
        environmentMode: EnvironmentMode = EnvironmentMode.SANDBOX
    ): OAuthRegisteredClient = OAuthRegisteredClient().apply {
        this.id = id
        this.clientId = clientId
        this.clientName = clientName
        this.domain = domain
        this.status = status
        this.clientSecret = clientSecret
        this.sandboxClientSecret = sandboxClientSecret
        this.productionClientSecret = productionClientSecret
        this.environmentMode = environmentMode
        this.clientIdIssuedAt = Instant.now()
        this.failedAuthAttempts = 0
    }

    fun pendingOrganization(
        id: String = UUID.randomUUID().toString(),
        domain: String = "pending.com"
    ): OAuthRegisteredClient = default(
        id = id,
        domain = domain,
        status = OrganizationStatus.PENDING
    )

    fun suspendedOrganization(
        id: String = UUID.randomUUID().toString(),
        domain: String = "suspended.com"
    ): OAuthRegisteredClient = default(
        id = id,
        domain = domain,
        status = OrganizationStatus.SUSPENDED
    )
}

object ProviderAccountBuilder {
    fun default(
        id: UUID? = UUID.randomUUID(),
        user: OAuthUser,
        provider: OAuthProvider = OAuthProvider.GOOGLE,
        providerUserId: String = "provider_user_123",
        providerEmail: String? = user.email,
        linkedAt: Instant = Instant.now(),
        lastLoginAt: Instant? = null
    ): OAuthProviderAccount = OAuthProviderAccount(
        id = id,
        user = user,
        provider = provider,
        providerUserId = providerUserId,
        providerEmail = providerEmail,
        linkedAt = linkedAt,
        lastLoginAt = lastLoginAt
    )

    fun googleAccount(
        user: OAuthUser,
        providerUserId: String = "google_user_123"
    ): OAuthProviderAccount = default(
        user = user,
        provider = OAuthProvider.GOOGLE,
        providerUserId = providerUserId
    )

    fun microsoftAccount(
        user: OAuthUser,
        providerUserId: String = "microsoft_user_123"
    ): OAuthProviderAccount = default(
        user = user,
        provider = OAuthProvider.MICROSOFT,
        providerUserId = providerUserId
    )
}

object ProcessBuilder {
    fun emailVerificationProcess(
        id: Long = 1L,
        publicId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        userEmail: String = "test@example.com",
        verificationToken: String = UUID.randomUUID().toString(),
        state: ProcessState = ProcessState.PENDING,
        channel: ProcessChannel = ProcessChannel.BUSINESS_WEB
    ): ProcessDto = ProcessDto(
        id = id,
        publicId = publicId,
        state = state,
        type = ProcessType.EMAIL_VERIFICATION,
        channel = channel,
        createdDate = Instant.now(),
        externalReference = verificationToken,
        requests = listOf(
            ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = state,
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to userId.toString()
                ),
                data = mapOf(
                    ProcessRequestDataName.USER_EMAIL to userEmail,
                    ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                    ProcessRequestDataName.VERIFICATION_TOKEN to verificationToken
                )
            )
        )
    )

    fun completedProcess(
        id: Long = 1L,
        publicId: UUID = UUID.randomUUID(),
        processType: ProcessType = ProcessType.EMAIL_VERIFICATION,
        channel: ProcessChannel = ProcessChannel.BUSINESS_WEB
    ): ProcessDto = ProcessDto(
        id = id,
        publicId = publicId,
        state = ProcessState.COMPLETE,
        type = processType,
        channel = channel,
        createdDate = Instant.now(),
        requests = listOf(
            ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
        )
    )

    fun pendingProcessRequest(
        id: Long = 1L,
        type: ProcessRequestType = ProcessRequestType.CREATE_NEW_PROCESS,
        data: Map<ProcessRequestDataName, String> = emptyMap(),
        stakeholders: Map<ProcessStakeholderType, String> = emptyMap()
    ): ProcessRequestDto = ProcessRequestDto(
        id = id,
        type = type,
        state = ProcessState.PENDING,
        stakeholders = stakeholders,
        data = data
    )
}

object CommandBuilder {
    fun registerUserCommand(
        email: String = "newuser@example.com",
        password: String = "SecurePassword123!",
        fullName: String = "New User",
        organizationName: String? = "Test Organization"
    ): RegisterUserCommand = RegisterUserCommand(
        email = email,
        password = password,
        fullName = fullName,
        organizationName = organizationName
    )

    fun verifyEmailCommand(
        token: String = UUID.randomUUID().toString()
    ): VerifyEmailCommand = VerifyEmailCommand(token = token)

    fun resendVerificationCommand(
        email: String = "test@example.com"
    ): ResendVerificationCommand = ResendVerificationCommand(email = email)
}

object OAuthConfigBuilder {
    fun scope(name: String = "openid"): OAuthScope = OAuthScope(id = null, name = name)

    fun authenticationMethod(name: String = "client_secret_basic"): OAuthAuthenticationMethod =
        OAuthAuthenticationMethod(id = null, name = name)

    fun grantType(name: String = "client_credentials"): OAuthGrantType =
        OAuthGrantType(id = null, name = name)
}
