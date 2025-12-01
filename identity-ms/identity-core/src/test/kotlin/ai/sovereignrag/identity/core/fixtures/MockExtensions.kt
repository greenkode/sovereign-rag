package ai.sovereignrag.identity.core.fixtures

import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthAuthenticationMethod
import ai.sovereignrag.identity.core.entity.OAuthGrantType
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthScope
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.integration.NotificationClient
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.OAuthClientConfigService
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

fun mockPasswordEncoder(): PasswordEncoder = mockk {
    every { encode(any()) } returns "encoded_password"
    every { matches(any(), any()) } returns true
}

fun mockMessageService(): MessageService = mockk {
    every { getMessage(any<String>()) } answers { firstArg<String>() }
    every { getMessage(any<String>(), *anyVararg()) } answers { firstArg<String>() }
}

fun mockOAuthClientConfigService(): OAuthClientConfigService = mockk {
    every { getAuthenticationMethod("client_secret_basic") } returns mockk(relaxed = true)
    every { getAuthenticationMethod("client_secret_post") } returns mockk(relaxed = true)
    every { getGrantType("client_credentials") } returns mockk(relaxed = true)
    every { getGrantType("refresh_token") } returns mockk(relaxed = true)
    every { getScope("openid") } returns mockk(relaxed = true)
    every { getScope("profile") } returns mockk(relaxed = true)
    every { getScope("email") } returns mockk(relaxed = true)
    every { getScope("read") } returns mockk(relaxed = true)
    every { getScope("write") } returns mockk(relaxed = true)
}

fun mockNotificationClient(): NotificationClient = mockk {
    every { sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
}

class UserRepositoryMockSetup(private val repository: OAuthUserRepository) {
    private val savedUserSlot = slot<OAuthUser>()

    fun userNotFound(email: String) {
        every { repository.findByEmail(email) } returns null
    }

    fun userExists(user: OAuthUser) {
        every { repository.findByEmail(user.email) } returns user
        every { repository.findById(user.id!!) } returns Optional.of(user)
    }

    fun userFoundById(user: OAuthUser) {
        every { repository.findById(user.id!!) } returns Optional.of(user)
    }

    fun userNotFoundById(userId: UUID) {
        every { repository.findById(userId) } returns Optional.empty()
    }

    fun captureAndReturnSavedUser(): CapturingSlot<OAuthUser> {
        every { repository.save(capture(savedUserSlot)) } answers {
            savedUserSlot.captured.apply {
                if (id == null) id = UUID.randomUUID()
            }
        }
        return savedUserSlot
    }

    fun saveReturnsInput() {
        every { repository.save(any()) } answers { firstArg() }
    }
}

class ClientRepositoryMockSetup(private val repository: OAuthRegisteredClientRepository) {
    fun clientNotFoundByDomain(domain: String) {
        every { repository.findByDomain(domain) } returns null
    }

    fun clientFoundByDomain(domain: String, client: OAuthRegisteredClient) {
        every { repository.findByDomain(domain) } returns client
    }

    fun saveReturnsInput() {
        every { repository.save(any()) } answers { firstArg() }
    }
}

class ProcessGatewayMockSetup(private val gateway: ProcessGateway) {
    fun noPendingProcess(userId: UUID, processType: ProcessType = ProcessType.EMAIL_VERIFICATION) {
        every { gateway.findLatestPendingProcessesByTypeAndForUserId(processType, userId) } returns null
    }

    fun pendingProcessExists(userId: UUID, process: ProcessDto, processType: ProcessType = ProcessType.EMAIL_VERIFICATION) {
        every { gateway.findLatestPendingProcessesByTypeAndForUserId(processType, userId) } returns process
    }

    fun processFoundByToken(token: String, process: ProcessDto, processType: ProcessType = ProcessType.EMAIL_VERIFICATION) {
        every { gateway.findPendingProcessByTypeAndExternalReference(processType, token) } returns process
    }

    fun processNotFoundByToken(token: String, processType: ProcessType = ProcessType.EMAIL_VERIFICATION) {
        every { gateway.findPendingProcessByTypeAndExternalReference(processType, token) } returns null
    }

    fun createProcessReturns(process: ProcessDto) {
        every { gateway.createProcess(any()) } returns process
    }

    fun makeRequestSucceeds() {
        every { gateway.makeRequest(any()) } just runs
    }
}

fun OAuthUserRepository.mockSetup(): UserRepositoryMockSetup = UserRepositoryMockSetup(this)

fun OAuthRegisteredClientRepository.mockSetup(): ClientRepositoryMockSetup = ClientRepositoryMockSetup(this)

fun ProcessGateway.mockSetup(): ProcessGatewayMockSetup = ProcessGatewayMockSetup(this)
