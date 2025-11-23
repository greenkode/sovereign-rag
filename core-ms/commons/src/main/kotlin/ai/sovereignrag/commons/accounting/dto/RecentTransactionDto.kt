package ai.sovereignrag.commons.accounting.dto

import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import java.time.Instant
import java.util.UUID
import javax.money.MonetaryAmount

data class RecentTransactionDto(
    val publicId: UUID,
    val createdDate: Instant,
    val type: TransactionType,
    val narration: String,
    val referenceNumber: String,
    val externalReference: String?,
    val customerId: String,
    val status: TransactionStatus,
    val amount: MonetaryAmount,
    val commission: MonetaryAmount,
    val senderAccountId: UUID,
    val recipientAccountId: UUID,
    val senderRunningBalance: MonetaryAmount,
    val recipientRunningBalance: MonetaryAmount,
    val billpayProductName: String?
)

data class RecentTransactionFilter(
    val type: TransactionType? = null,
    val status: TransactionStatus? = null,
    val senderAccountId: UUID? = null,
    val recipientAccountId: UUID? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null
)