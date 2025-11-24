package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.exception.PropertyNotFoundException
import javax.money.MonetaryAmount

data class FundsTransferRequestPayload(
    val amount: MonetaryAmount,
    val narration: String,
    val reference: String,
    val additionalFields: Map<String, String>
) {
    fun getPropertyOrThrow(propertyName: String): String {
        return additionalFields[propertyName] ?: throw PropertyNotFoundException(propertyName)
    }
}