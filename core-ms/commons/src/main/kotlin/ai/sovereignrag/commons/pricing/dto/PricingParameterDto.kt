package ai.sovereignrag.commons.pricing.dto

import ai.sovereignrag.commons.accounting.AccountDto
import ai.sovereignrag.commons.accounting.TransactionType
import java.time.Instant
import java.util.UUID
import javax.money.MonetaryAmount

data class PricingParameterDto(
    val amount: MonetaryAmount, val transactionType: TransactionType,
    val accountCharged: AccountDto, val transactionTime: Instant, val integratorId: String? = null, val productId: UUID? = null
)