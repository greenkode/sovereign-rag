package ai.sovereignrag.commons.user.dto

import java.util.UUID

data class CreateExternalIdPayload(
    val userId: UUID,
    val externalId: String,
    val integratorId: String
)