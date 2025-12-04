package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.identity.core.deletion.command.DeleteUserCommand
import ai.sovereignrag.identity.core.deletion.command.DeleteUserCommandHandler
import ai.sovereignrag.identity.core.deletion.service.UserDeletionResult
import ai.sovereignrag.identity.core.deletion.service.UserDeletionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteUserCommandHandlerTest {

    private lateinit var userDeletionService: UserDeletionService
    private lateinit var handler: DeleteUserCommandHandler

    @BeforeEach
    fun setUp() {
        userDeletionService = mockk()
        handler = DeleteUserCommandHandler(userDeletionService)
    }

    @Test
    fun `should successfully delete user`() {
        val userId = UUID.randomUUID()
        val command = DeleteUserCommand(userId = userId)

        every { userDeletionService.deleteUser(userId) } returns UserDeletionResult(
            success = true,
            message = "user.deleted_successfully",
            userId = userId
        )

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("user.deleted_successfully", result.message)
        assertEquals(userId, result.userId)
        verify(exactly = 1) { userDeletionService.deleteUser(userId) }
    }

    @Test
    fun `should return failure when user not found`() {
        val userId = UUID.randomUUID()
        val command = DeleteUserCommand(userId = userId)

        every { userDeletionService.deleteUser(userId) } returns UserDeletionResult(
            success = false,
            message = "error.user_not_found",
            userId = userId
        )

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("error.user_not_found", result.message)
        assertEquals(userId, result.userId)
        verify(exactly = 1) { userDeletionService.deleteUser(userId) }
    }
}
