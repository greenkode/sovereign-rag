package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction

data class BillPaymentStatusResult(
    val transactionId: String,
    val action: TransactionFollowUpAction,
    val creditToken: String? = null
)
