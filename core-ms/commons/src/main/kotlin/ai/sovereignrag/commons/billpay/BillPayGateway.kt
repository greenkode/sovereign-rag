package ai.sovereignrag.commons.billpay

import ai.sovereignrag.commons.billpay.dto.BillPayCustomerDetailsResult
import ai.sovereignrag.commons.billpay.dto.PayBillRequestPayload
import ai.sovereignrag.commons.billpay.dto.PayBillResultDto
import ai.sovereignrag.commons.user.dto.MerchantDetailsDto
import java.util.UUID
import javax.money.CurrencyUnit

data class BillPayVendorInfo(
    val id: String,
    val name: String,
    val poolAccount: UUID?
)

interface BillPayGateway {

    fun getCustomerDetails(vendorId: UUID, accountNumber: String): BillPayCustomerDetailsResult

    fun payBill(payBillRequestPayload: PayBillRequestPayload): PayBillResultDto

    fun getStatus(externalReference: String, integratorId: String): PayBillResultDto

    fun getPoolAccount(integratorId: String, currency: CurrencyUnit): UUID?

    fun handleRemotePaymentResult(result: PayBillResultDto, merchantDetails: MerchantDetailsDto)
    
    fun getAllVendorInfo(currency: CurrencyUnit): List<BillPayVendorInfo>
}