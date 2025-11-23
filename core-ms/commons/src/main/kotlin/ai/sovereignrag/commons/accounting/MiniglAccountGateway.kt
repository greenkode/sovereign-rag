package ai.sovereignrag.commons.accounting

import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

interface MiniglAccountGateway {

    fun getAccountBalance(code: String): AccountBalanceDto

    fun createAccount(
        currency: CurrencyUnit,
        publicId: UUID,
        type: AccountType,
        metadata: Map<String, String>
    ): CreatedAccountDetails

    fun getAccountBalances(ids: Set<String>): Map<UUID, MonetaryAmount>

    fun getRunningBalanceByCode(code: String, transactionReference: UUID): MonetaryAmount
}

data class AccountBalanceDto(
    val accountId: Long,
    val balance: MonetaryAmount
)

data class CreatedAccountDetails(
    val id: Long,
    val number: String,
    val currency: CurrencyUnit,
    val balance: MonetaryAmount
)