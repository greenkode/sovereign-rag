package ai.sovereignrag.commons.accounting.dto

import java.util.UUID
import javax.money.MonetaryAmount

data class MerchantTransactionAnalyticsDto(
    val merchantId: UUID,
    val totalTransactions: Long,
    val averageTransactionAmount: MonetaryAmount,
    val failedTransactionPercentage: Double,
    val refundedTransactionPercentage: Double
)