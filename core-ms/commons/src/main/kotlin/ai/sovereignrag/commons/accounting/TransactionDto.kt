package ai.sovereignrag.commons.accounting

import java.time.Instant
import java.util.UUID
import javax.money.MonetaryAmount

data class TransactionDto(
    val internalReference: UUID,
    val externalReference: String?,
    val processId: UUID,
    val displayRef: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val description: String,
    val narration: String? = null,
    val senderName: String,
    val amount: MonetaryAmount,
    val createdDate: Instant,
    val senderAccountId: UUID,
    val recipientAccountId: UUID,
    val senderRunningBalance: MonetaryAmount,
    val recipientRunningBalance: MonetaryAmount,
    val properties: List<TransactionPropertyDto> = emptyList()
)

data class TransactionReferenceDto(
    val internalReference: UUID,
    val displayReference: String,
    val type: TransactionType,
)