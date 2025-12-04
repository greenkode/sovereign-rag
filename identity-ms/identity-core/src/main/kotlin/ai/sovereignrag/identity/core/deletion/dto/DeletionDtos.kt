package ai.sovereignrag.identity.core.deletion.dto

import java.util.UUID

data class DeleteUserRequest(
    val userId: String
)

data class DeleteUserResponse(
    val success: Boolean,
    val message: String,
    val userId: UUID
)

data class DeleteOrganizationRequest(
    val organizationId: String
)

data class DeleteOrganizationResponse(
    val success: Boolean,
    val message: String,
    val organizationId: UUID,
    val usersDeleted: Int
)
