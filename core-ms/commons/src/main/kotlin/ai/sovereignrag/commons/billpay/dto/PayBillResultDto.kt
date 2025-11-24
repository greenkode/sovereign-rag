package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.enumeration.ResponseCode
import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction
import java.util.UUID

data class PayBillResultDto(
    val reference: String,
    val processReference: UUID,
    val integratorReference: String,
    val action: TransactionFollowUpAction,
    val responseCode: ResponseCode,
    val token: String? = null,
    val additionalInfo: Map<String, String> = mapOf(),
)
