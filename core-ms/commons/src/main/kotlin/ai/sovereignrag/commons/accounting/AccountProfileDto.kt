package ai.sovereignrag.commons.accounting

import java.util.UUID

data class AccountProfileDto(
    val name: String,
    val publicId: UUID
) : java.io.Serializable