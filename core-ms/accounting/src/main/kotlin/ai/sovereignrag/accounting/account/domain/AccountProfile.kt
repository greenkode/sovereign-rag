package ai.sovereignrag.accounting.account.domain

import ai.sovereignrag.commons.accounting.AccountProfileDto
import java.util.UUID

data class AccountProfile(
    val id: Int, val name: String, val description: String, val publicId: UUID
) {
    fun toDto(): AccountProfileDto {
        return AccountProfileDto(name, publicId)
    }
}