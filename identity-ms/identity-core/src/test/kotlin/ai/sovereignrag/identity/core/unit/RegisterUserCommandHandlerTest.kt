package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.ProcessRequestDto
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.integration.NotificationClient
import ai.sovereignrag.identity.core.registration.command.RegisterUserCommand
import ai.sovereignrag.identity.core.registration.command.RegisterUserCommandHandler
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.BusinessEmailValidationService
import ai.sovereignrag.identity.core.service.OAuthClientConfigService
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
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegisterUserCommandHandlerTest {

    private val userRepository: OAuthUserRepository = mockk()
    private val oauthClientRepository: OAuthRegisteredClientRepository = mockk()
    private val businessEmailValidationService: BusinessEmailValidationService = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val processGateway: ProcessGateway = mockk()
    private val messageService: MessageService = mockk()
    private val oauthClientConfigService: OAuthClientConfigService = mockk()
    private val notificationClient: NotificationClient = mockk()

    private lateinit var handler: RegisterUserCommandHandler

    @BeforeEach
    fun setup() {
        handler = RegisterUserCommandHandler(
            userRepository,
            oauthClientRepository,
            businessEmailValidationService,
            passwordEncoder,
            processGateway,
            messageService,
            oauthClientConfigService,
            notificationClient,
            "http://localhost:3000/auth/verify-email"
        )
    }

    @Test
    fun `should register user with new organization successfully`() {
        val command = createCommand()
        val userSlot = slot<OAuthUser>()

        setupMocksForNewOrganization()
        every { userRepository.findByEmail("john.doe@acme.com") } returns null
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Registration successful", result.message)
        assertNotNull(result.userId)
        assertNotNull(result.organizationId)
        assertTrue(result.isNewOrganization)
        assertTrue(result.verificationRequired)

        val savedUser = userSlot.captured
        assertEquals("john.doe@acme.com", savedUser.email)
        assertEquals("John", savedUser.firstName)
        assertEquals("Doe", savedUser.lastName)
        assertEquals(false, savedUser.emailVerified)
        assertTrue(savedUser.authorities.contains("ROLE_ADMIN"))

        verify { processGateway.createProcess(any()) }
        verify { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should register user with existing organization`() {
        val command = createCommand()
        val existingClient = mockk<OAuthRegisteredClient>()
        val organizationId = UUID.randomUUID()
        val userSlot = slot<OAuthUser>()

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { userRepository.findByEmail("john.doe@acme.com") } returns null
        every { oauthClientRepository.findByDomain("acme.com") } returns existingClient
        every { existingClient.id } returns organizationId.toString()
        every { passwordEncoder.encode(any()) } returns "encoded_password"
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { processGateway.findLatestPendingProcessesByTypeAndForUserId(any(), any()) } returns null
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals(organizationId, result.organizationId)
        assertEquals(false, result.isNewOrganization)

        val savedUser = userSlot.captured
        assertEquals(false, savedUser.authorities.contains("ROLE_ADMIN"))
    }

    @Test
    fun `should throw exception when email already exists with complete registration`() {
        val command = createCommand()
        val existingUser = OAuthUser().apply {
            id = UUID.randomUUID()
            email = "john.doe@acme.com"
            registrationComplete = true
        }

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { userRepository.findByEmail("john.doe@acme.com") } returns existingUser
        every { messageService.getMessage("registration.error.email_exists") } returns "Email already exists"

        val exception = assertThrows<ClientException> {
            handler.handle(command)
        }

        assertEquals("Email already exists", exception.message)
    }

    @Test
    fun `should allow re-registration when registration is incomplete`() {
        val command = createCommand()
        val existingUserId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val existingUser = OAuthUser().apply {
            id = existingUserId
            email = "john.doe@acme.com"
            this.merchantId = merchantId
            registrationComplete = false
            emailVerified = false
        }
        val userSlot = slot<OAuthUser>()

        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { userRepository.findByEmail("john.doe@acme.com") } returns existingUser
        every { oauthClientRepository.findByDomain("acme.com") } returns null
        every { passwordEncoder.encode(any()) } returns "encoded_password"
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        every { processGateway.findLatestPendingProcessesByTypeAndForUserId(any(), any()) } returns null
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals(existingUserId, result.userId)
        assertEquals(merchantId, result.organizationId)
        assertEquals(false, result.isNewOrganization)

        val savedUser = userSlot.captured
        assertEquals("John", savedUser.firstName)
        assertEquals("Doe", savedUser.lastName)
        assertEquals(false, savedUser.registrationComplete)
    }

    @Test
    fun `should normalize email to lowercase`() {
        val command = RegisterUserCommand(
            email = "John.Doe@ACME.com",
            password = "SecurePassword123!",
            fullName = "John Doe",
            organizationName = null
        )
        val userSlot = slot<OAuthUser>()

        setupMocksForNewOrganization()
        every { userRepository.findByEmail("john.doe@acme.com") } returns null
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        handler.handle(command)

        assertEquals("john.doe@acme.com", userSlot.captured.email)
    }

    @Test
    fun `should parse full name correctly with multiple parts`() {
        val command = RegisterUserCommand(
            email = "john.doe@acme.com",
            password = "SecurePassword123!",
            fullName = "John Michael Doe",
            organizationName = null
        )
        val userSlot = slot<OAuthUser>()

        setupMocksForNewOrganization()
        every { userRepository.findByEmail("john.doe@acme.com") } returns null
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        handler.handle(command)

        assertEquals("John", userSlot.captured.firstName)
        assertEquals("Michael Doe", userSlot.captured.lastName)
    }

    @Test
    fun `should handle single name without last name`() {
        val command = RegisterUserCommand(
            email = "john@acme.com",
            password = "SecurePassword123!",
            fullName = "John",
            organizationName = null
        )
        val userSlot = slot<OAuthUser>()

        setupMocksForNewOrganization()
        every { userRepository.findByEmail("john@acme.com") } returns null
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = UUID.randomUUID() } }
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.user_created") } returns "Registration successful"

        handler.handle(command)

        assertEquals("John", userSlot.captured.firstName)
        assertEquals("", userSlot.captured.lastName)
    }

    private fun createCommand() = RegisterUserCommand(
        email = "john.doe@acme.com",
        password = "SecurePassword123!",
        fullName = "John Doe",
        organizationName = "Acme Corporation"
    )

    private fun setupMocksForNewOrganization() {
        every { businessEmailValidationService.validateBusinessEmail(any()) } just runs
        every { oauthClientRepository.findByDomain(any()) } returns null
        every { passwordEncoder.encode(any()) } returns "encoded_password"
        every { oauthClientRepository.save(any()) } answers { firstArg() }
        every { processGateway.findLatestPendingProcessesByTypeAndForUserId(any(), any()) } returns null

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

    private fun createMockProcessDto(): ProcessDto {
        return ProcessDto(
            id = 1L,
            publicId = UUID.randomUUID(),
            state = ProcessState.PENDING,
            type = ProcessType.EMAIL_VERIFICATION,
            channel = Channel.BUSINESS_WEB,
            createdDate = Instant.now(),
            requests = listOf(
                ProcessRequestDto(
                    id = 1L,
                    type = ProcessRequestType.CREATE_NEW_PROCESS,
                    state = ProcessState.PENDING,
                    stakeholders = emptyMap(),
                    data = emptyMap()
                )
            )
        )
    }
}
