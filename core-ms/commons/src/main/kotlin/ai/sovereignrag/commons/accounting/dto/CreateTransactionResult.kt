package ai.sovereignrag.commons.accounting.dto

import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import java.util.UUID
import javax.money.MonetaryAmount

data class CreateTransactionResult(
    val shortRef: String,
    val internalReference: UUID,
    val amount: MonetaryAmount,
    val type: TransactionType,
    val fee: MonetaryAmount,
    val commission: MonetaryAmount,
    val status: TransactionStatus
)
