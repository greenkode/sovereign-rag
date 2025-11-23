package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

interface MiniglTransactionGateway {
    
    fun createTransaction(
        transaction: MiniglTransactionDto,
        pending: Boolean,
        entries: ArrayList<LedgerEntryDto>,
        transactionLimit: TransactionLimitDto? = null,
    ): CreateTransactionDetailsDto
    
    fun completePendingTransaction(reference: UUID): CreateTransactionDetailsDto

    fun reverseTransaction(reference: UUID, reversalReference: UUID): CreateTransactionDetailsDto
}

data class MiniglTransactionDto(
    val reference: UUID,
    val type: String, 
    val displayRef: String,
    val customerDetails: String,
    val fee: MonetaryAmount,
    val commission: MonetaryAmount,
    val rebate: MonetaryAmount,
    val processPublicId: String?
)

data class TransactionLimitDto(
    val transactionType: String,
    val accountType: String,
    val currency: CurrencyUnit,
    val maxDailyDebit: MonetaryAmount,
    val maxDailyCredit: MonetaryAmount,
    val cumulativeCredit: MonetaryAmount,
    val cumulativeDebit: MonetaryAmount,
    val minTransactionDebit: MonetaryAmount,
    val minTransactionCredit: MonetaryAmount,
    val maxTransactionDebit: MonetaryAmount,
    val maxTransactionCredit: MonetaryAmount,
    val maxAccountBalance: MonetaryAmount
)

data class CreateTransactionDetailsDto(
    val reference: UUID,
    val metadata: Map<String, String>
)