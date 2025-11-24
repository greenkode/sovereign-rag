package ai.sovereignrag.accounting.account.domain

import ai.sovereignrag.commons.accounting.AccountAddressDto
import ai.sovereignrag.commons.accounting.AccountAddressPropertyName
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.AddressPropertyDto
import javax.money.CurrencyUnit

data class AccountAddress(
    val address: String,
    val type: AccountAddressType,
    val platform: String,
    val currency: CurrencyUnit,
    val properties: Set<AddressProperty> = emptySet()
) {
    fun toDto() = AccountAddressDto(address, type, platform, currency, properties.map { it.toDto() }.toSet())
}

data class AddressProperty(
    val name: AccountAddressPropertyName,
    val value: String
) {
    fun toDto(): AddressPropertyDto {
        return AddressPropertyDto(name, value)
    }
}