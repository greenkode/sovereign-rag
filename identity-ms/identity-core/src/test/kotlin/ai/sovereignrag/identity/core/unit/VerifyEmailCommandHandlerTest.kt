package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.commons.process.ProcessDto
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.ProcessRequestDto
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.registration.command.VerifyEmailCommand
import ai.sovereignrag.identity.core.registration.command.VerifyEmailCommandHandler
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifyEmailCommandHandlerTest {

    private val processGateway: ProcessGateway = mockk()
    private val userRepository: OAuthUserRepository = mockk()
    private val messageService: MessageService = mockk()

    private lateinit var handler: VerifyEmailCommandHandler

    private val userId = UUID.randomUUID()
    private val processId = UUID.randomUUID()
    private val token = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        handler = VerifyEmailCommandHandler(
            processGateway,
            userRepository,
            messageService
        )
    }

    @Test
    fun `should verify email successfully`() {
        val command = VerifyEmailCommand(token = token)
        val process = createMockProcess(userId.toString())
        val user = createMockUser()

        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, token) } returns process
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any()) } returns user
        every { processGateway.makeRequest(any()) } just runs
        every { messageService.getMessage("registration.success.email_verified") } returns "Email verified successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Email verified successfully", result.message)
        assertEquals(userId, result.userId)

        verify { userRepository.save(user) }
        verify { processGateway.makeRequest(any()) }
        assertEquals(true, user.emailVerified)
    }

    @Test
    fun `should throw exception when token is invalid`() {
        val command = VerifyEmailCommand(token = "invalid-token")

        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, "invalid-token") } returns null
        every { messageService.getMessage("registration.error.invalid_verification_token") } returns "Invalid token"

        val exception = assertThrows<ClientException> {
            handler.handle(command)
        }

        assertEquals("Invalid token", exception.message)
    }

    @Test
    fun `should throw exception when user not found in process data`() {
        val command = VerifyEmailCommand(token = token)
        val process = createMockProcess(null)

        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, token) } returns process
        every { messageService.getMessage("registration.error.user_not_found") } returns "User not found"

        val exception = assertThrows<ClientException> {
            handler.handle(command)
        }

        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should throw exception when user does not exist in database`() {
        val command = VerifyEmailCommand(token = token)
        val process = createMockProcess(userId.toString())

        every { processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.EMAIL_VERIFICATION, token) } returns process
        every { userRepository.findById(userId) } returns Optional.empty()
        every { messageService.getMessage("registration.error.user_not_found") } returns "User not found"

        assertThrows<Exception> {
            handler.handle(command)
        }
    }

    private fun createMockProcess(userIdValue: String?): ProcessDto {
        val dataMap = mutableMapOf<ProcessRequestDataName, String>()
        userIdValue?.let { dataMap[ProcessRequestDataName.USER_IDENTIFIER] = it }

        return ProcessDto(
            id = 1L,
            publicId = processId,
            state = ProcessState.PENDING,
            type = ProcessType.EMAIL_VERIFICATION,
            channel = Channel.BUSINESS_WEB,
            createdDate = java.time.Instant.now(),
            requests = listOf(
                ProcessRequestDto(
                    id = 1L,
                    type = ProcessRequestType.CREATE_NEW_PROCESS,
                    state = ProcessState.PENDING,
                    stakeholders = emptyMap(),
                    data = dataMap
                )
            )
        )
    }

    private fun createMockUser(): OAuthUser {
        return OAuthUser().apply {
            id = userId
            email = "test@example.com"
            emailVerified = false
        }
    }
}
