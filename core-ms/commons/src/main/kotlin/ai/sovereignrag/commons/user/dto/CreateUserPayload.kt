package ai.sovereignrag.commons.user.dto

import java.util.*

data class CreateUserPayload(
    val userId: UUID,
    val externalIds: MutableSet<UserExternalId> = mutableSetOf(),
    val userProperties: MutableSet<UserProperty> = mutableSetOf()
)