package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.process.ProcessChannel
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

enum class TrustLevel { LOW, MEDIUM, HIGH }
enum class UserType { INDIVIDUAL, MERCHANT, AGENT, SYSTEM }
data class MerchantDetailsDto(val name: String, val category: String)


data class AccountDto(
    val name: String,
    val type: AccountType,
    val publicId: UUID,
    val accountId: Long,
    val alias: String,
    val userId: UUID,
    val status: AccountStatus,
    val profile: AccountProfileDto,
    val currency: CurrencyUnit,
    val addresses: Set<AccountAddressDto>,
) : java.io.Serializable

data class AccountAddressDto(
    val address: String,
    val type: AccountAddressType,
    val platform: String,
    val currency: CurrencyUnit,
    val properties: Set<AddressPropertyDto> = emptySet()
) : java.io.Serializable

data class AccountCreatedEvent(
    val publicId: UUID,
    val accountId: Long,
    val name: String,
    val userId: UUID,
    val userType: UserType,
    val parameters: Map<String, Any>,
    val alias: String,
    val currency: CurrencyUnit,
    val currencyIssuer: String? = null,
    val channel: ProcessChannel,
    val merchantDetails: MerchantDetailsDto? = null,
)

data class CreateAccountPayload(
    val name: String,
    val trustLevel: TrustLevel,
    val userId: UUID,
    val currency: CurrencyUnit,
    val alias: String,
    val type: AccountType,
    val publicId: UUID = UUID.randomUUID(),
    val allowMultipleOfSameCurrency: Boolean = false,
    val addresses: Set<AccountAddressDto> = setOf(),
)

data class AddressPropertyDto(
    val name: AccountAddressPropertyName,
    val value: String,
) : java.io.Serializable

enum class AccountAddressPropertyName {
    CURRENCY_ISSUER,
    BANK_CODE
}

enum class AccountPropertyName {
    ACCOUNT_BALANCE_MANDATE
}

enum class AccountPropertyScope {
    GLOBAL,
    BUSINESS,
    INDIVIDUAL
}


