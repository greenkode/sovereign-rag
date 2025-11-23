package ai.sovereignrag.commons.accounting.dto

import ai.sovereignrag.commons.accounting.AccountDto
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class CreateTransactionPayload(
    val amount: MonetaryAmount,
    val reference: UUID,
    val transactionType: TransactionType,
    val transactionStatus: TransactionStatus,
    val processPublicId: UUID,
    val senderAccountAddress: String,
    val recipientAccountAddress: String,
    val customerDetails: String,
    val entries: List<LedgerEntryDto>,
    val integratorId: String? = null,
    val externalReference: String? = null,
    val productId: UUID? = null,
    val isLend: Boolean? = false,
)

data class LedgerEntryDto(
    val type: EntryType,
    val amount: MonetaryAmount,
    val debitAccount: AccountDto,
    val creditAccount: AccountDto,
    val currencyUnit: CurrencyUnit,
    val narration: String,
    val details: String,
)