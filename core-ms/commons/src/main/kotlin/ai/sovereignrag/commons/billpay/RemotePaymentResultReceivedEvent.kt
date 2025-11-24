package ai.sovereignrag.commons.billpay

import ai.sovereignrag.commons.billpay.dto.PayBillResultDto
import ai.sovereignrag.commons.user.dto.MerchantDetailsDto

data class RemotePaymentResultReceivedEvent(val result: PayBillResultDto, val merchantDetails: MerchantDetailsDto)