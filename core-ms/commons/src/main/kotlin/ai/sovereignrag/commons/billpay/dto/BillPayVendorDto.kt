package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.billpay.VendorPropertyName
import ai.sovereignrag.commons.billpay.BillPayVendorStatus
import java.util.UUID
import javax.money.MonetaryAmount

data class BillPayVendorDto(
    val name: String, val publicId: UUID,
    val minimumAmount: MonetaryAmount, val maximumAmount: MonetaryAmount,
    val status: BillPayVendorStatus,
    val properties: Map<VendorPropertyName, String>,
    val iconUrl: String?
) : java.io.Serializable