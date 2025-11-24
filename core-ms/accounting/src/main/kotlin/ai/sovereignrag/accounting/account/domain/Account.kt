package ai.sovereignrag.accounting.account.domain

import ai.sovereignrag.commons.accounting.AccountDto
import ai.sovereignrag.commons.accounting.AccountPropertyName
import ai.sovereignrag.commons.accounting.AccountStatus
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.kyc.TrustLevel
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class Account(
    val id: Long,
    val name: String,
    val alias: String,
    val publicId: UUID,
    val trustLevel: TrustLevel,
    val userId: UUID,
    val currency: CurrencyUnit,
    val type: AccountType,
    val status: AccountStatus,
    val default: Boolean,
    val profile: AccountProfile,
    val addresses: Set<AccountAddress> = setOf(),
    val properties: Map<AccountPropertyName, String> = mapOf(),
    val parentId: Long? = null,
) {
    fun toDto(): AccountDto {
        return AccountDto(
            name,
            type,
            publicId,
            id,
            alias,
            userId,
            status,
            profile.toDto(),
            currency,
            addresses.map { it.toDto() }.toSet()
        )
    }
}
