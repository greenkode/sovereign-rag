package ai.sovereignrag.accounting.transaction.domain

import ai.sovereignrag.accounting.transaction.domain.model.TransactionProperty
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.currency.DefaultCurrency
import org.javamoney.moneta.Money
import java.time.Instant
import java.util.UUID
import javax.money.MonetaryAmount

data class Transaction(
    val amount: MonetaryAmount,
    val fee: MonetaryAmount,
    val reference: UUID,
    val externalReference: String?,
    val id: Long,
    val lend: Boolean,
    val processPublicId: UUID,
    val displayRef: String,
    val commission: MonetaryAmount,
    val rebate: MonetaryAmount,
    val type: TransactionType,
    val status: TransactionStatus,
    val createdDate: Instant,
    val customerDetails: String,
    val senderAccountId: UUID,
    val recipientAccountId: UUID,
    val properties: Set<TransactionProperty>,
    val senderRunningBalance: MonetaryAmount,
    val recipientRunningBalance: MonetaryAmount
)
