package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.identity.core.deletion.command.DeleteOrganizationCommand
import ai.sovereignrag.identity.core.deletion.command.DeleteOrganizationCommandHandler
import ai.sovereignrag.identity.core.deletion.service.OrganizationDeletionResult
import ai.sovereignrag.identity.core.deletion.service.OrganizationDeletionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteOrganizationCommandHandlerTest {

    private lateinit var organizationDeletionService: OrganizationDeletionService
    private lateinit var handler: DeleteOrganizationCommandHandler

    @BeforeEach
    fun setUp() {
        organizationDeletionService = mockk()
        handler = DeleteOrganizationCommandHandler(organizationDeletionService)
    }

    @Test
    fun `should successfully delete organization and all users`() {
        val organizationId = UUID.randomUUID()
        val command = DeleteOrganizationCommand(organizationId = organizationId)

        every { organizationDeletionService.deleteOrganization(organizationId) } returns OrganizationDeletionResult(
            success = true,
            message = "organization.deleted_successfully",
            organizationId = organizationId,
            usersDeleted = 5
        )

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("organization.deleted_successfully", result.message)
        assertEquals(organizationId, result.organizationId)
        assertEquals(5, result.usersDeleted)
        verify(exactly = 1) { organizationDeletionService.deleteOrganization(organizationId) }
    }

    @Test
    fun `should return failure when organization not found`() {
        val organizationId = UUID.randomUUID()
        val command = DeleteOrganizationCommand(organizationId = organizationId)

        every { organizationDeletionService.deleteOrganization(organizationId) } returns OrganizationDeletionResult(
            success = false,
            message = "error.organization_not_found",
            organizationId = organizationId,
            usersDeleted = 0
        )

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("error.organization_not_found", result.message)
        assertEquals(organizationId, result.organizationId)
        assertEquals(0, result.usersDeleted)
        verify(exactly = 1) { organizationDeletionService.deleteOrganization(organizationId) }
    }

    @Test
    fun `should return failure when organization already deleted`() {
        val organizationId = UUID.randomUUID()
        val command = DeleteOrganizationCommand(organizationId = organizationId)

        every { organizationDeletionService.deleteOrganization(organizationId) } returns OrganizationDeletionResult(
            success = false,
            message = "error.organization_already_deleted",
            organizationId = organizationId,
            usersDeleted = 0
        )

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("error.organization_already_deleted", result.message)
        assertEquals(0, result.usersDeleted)
        verify(exactly = 1) { organizationDeletionService.deleteOrganization(organizationId) }
    }
}
