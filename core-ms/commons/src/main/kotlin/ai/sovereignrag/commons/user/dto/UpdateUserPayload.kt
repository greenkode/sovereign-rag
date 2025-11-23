package ai.sovereignrag.commons.user.dto

import java.util.*

data class UpdateUserPayload(
    val userId: UUID,
    val externalIds: Set<UserExternalId> = emptySet(),
    val userProperties: Set<UserProperty> = emptySet()
)