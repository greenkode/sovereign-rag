package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.enumeration.AdditionalInfoParameters

data class BillPayCustomerDetailsResult(val name: String, val accountNumber: String, val additionalInfo: Map<AdditionalInfoParameters, String> = mapOf(),
    val customerAddress: String? = null
)