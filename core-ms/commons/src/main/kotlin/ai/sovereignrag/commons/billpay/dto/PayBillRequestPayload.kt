package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.accounting.TransactionDto
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.user.dto.PhoneNumber
import java.util.UUID
import javax.money.MonetaryAmount

data class PayBillRequestPayload(
    val accountNumber: String,
    val phoneNumber: PhoneNumber,
    val isLend: Boolean,
    val product: BillPayProductDto,
    val amount: MonetaryAmount,
    val accountPublicId: UUID,
    val userId: UUID,
    val process: ProcessDto,
    val pendingTransaction: TransactionDto
)
