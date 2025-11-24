package ai.sovereignrag.accounting.limit.domain

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class TransactionLimit(
    val id: Int,
    val profileId: UUID,
    val transactionType: TransactionType,
    val accountType: AccountType,
    val currency: CurrencyUnit,
    val maxDailyDebit: MonetaryAmount,
    val maxDailyCredit: MonetaryAmount,
    val cumulativeCredit: MonetaryAmount,
    val cumulativeDebit: MonetaryAmount,
    val minTransactionDebit: MonetaryAmount,
    val minTransactionCredit: MonetaryAmount,
    val maxTransactionDebit: MonetaryAmount,
    val maxTransactionCredit: MonetaryAmount,
    val maxAccountBalance: MonetaryAmount,

    val start: Instant,

    val expiry: Instant?
)