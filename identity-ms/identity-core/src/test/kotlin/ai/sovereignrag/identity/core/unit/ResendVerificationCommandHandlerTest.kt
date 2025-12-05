package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessRequestDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.integration.NotificationClient
import ai.sovereignrag.identity.core.registration.command.ResendVerificationCommand
import ai.sovereignrag.identity.core.registration.command.ResendVerificationCommandHandler
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResendVerificationCommandHandlerTest {

    private val userRepository: OAuthUserRepository = mockk()
    private val processGateway: ProcessGateway = mockk()
    private val messageService: MessageService = mockk()
    private val notificationClient: NotificationClient = mockk()

    private lateinit var handler: ResendVerificationCommandHandler

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = ResendVerificationCommandHandler(
            userRepository,
            processGateway,
            messageService,
            notificationClient,
            "http://localhost:3000/auth/verify-email"
        )
    }

    @Test
    fun `should resend verification email successfully`() {
        val command = ResendVerificationCommand(email = "test@example.com")
        val user = createMockUser(emailVerified = false)

        every { userRepository.findByEmail("test@example.com") } returns user
        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, "test@example.com") } returns null
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.verification_resent") } returns "Verification email sent"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Verification email sent", result.message)

        verify { processGateway.createProcess(any()) }
        verify { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should cancel existing process before creating new one`() {
        val command = ResendVerificationCommand(email = "test@example.com")
        val user = createMockUser(emailVerified = false)
        val existingProcess = createMockProcessDto()

        every { userRepository.findByEmail("test@example.com") } returns user
        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, "test@example.com") } returns existingProcess
        every { processGateway.makeRequest(any()) } just runs
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.verification_resent") } returns "Verification email sent"

        val result = handler.handle(command)

        assertTrue(result.success)
        verify(exactly = 1) { processGateway.makeRequest(any()) }
        verify(exactly = 1) { processGateway.createProcess(any()) }
    }

    @Test
    fun `should throw exception when user not found`() {
        val command = ResendVerificationCommand(email = "nonexistent@example.com")

        every { userRepository.findByEmail("nonexistent@example.com") } returns null
        every { messageService.getMessage("registration.error.user_not_found") } returns "User not found"

        val exception = assertThrows<InvalidRequestException> {
            handler.handle(command)
        }

        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should throw exception when email already verified`() {
        val command = ResendVerificationCommand(email = "test@example.com")
        val user = createMockUser(emailVerified = true)

        every { userRepository.findByEmail("test@example.com") } returns user
        every { messageService.getMessage("registration.error.email_already_verified") } returns "Email already verified"

        val exception = assertThrows<InvalidRequestException> {
            handler.handle(command)
        }

        assertEquals("Email already verified", exception.message)
    }

    @Test
    fun `should normalize email to lowercase`() {
        val command = ResendVerificationCommand(email = "TEST@EXAMPLE.COM")
        val user = createMockUser(emailVerified = false)

        every { userRepository.findByEmail("test@example.com") } returns user
        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, "test@example.com") } returns null
        every { processGateway.createProcess(any()) } returns createMockProcessDto()
        every { notificationClient.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()) } returns null
        every { messageService.getMessage("registration.success.verification_resent") } returns "Verification email sent"

        handler.handle(command)

        verify { userRepository.findByEmail("test@example.com") }
    }

    private fun createMockUser(emailVerified: Boolean): OAuthUser {
        return OAuthUser().apply {
            id = userId
            email = "test@example.com"
            firstName = "Test"
            lastName = "User"
            this.emailVerified = emailVerified
        }
    }

    private fun createMockProcessDto(): ProcessDto {
        return ProcessDto(
            id = 1L,
            publicId = UUID.randomUUID(),
            state = ProcessState.PENDING,
            type = ProcessType.EMAIL_VERIFICATION,
            channel = ProcessChannel.BUSINESS_WEB,
            createdDate = java.time.Instant.now(),
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
