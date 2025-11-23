package ai.sovereignrag.commons.accounting.dto

import java.util.UUID
import javax.money.MonetaryAmount

data class MerchantTransactionStatisticsDto(
    val merchantId: UUID,
    val merchantName: String,
    val totalTransactions: Long,
    val successfulTransactions: Long,
    val failedTransactions: Long,
    val pendingTransactions: Long,
    val reversedTransactions: Long,
    val lendingTransactions: Long,
    val nonLendingTransactions: Long,
    val totalAmount: MonetaryAmount,
    val successfulAmount: MonetaryAmount,
    val failedAmount: MonetaryAmount,
    val lendingAmount: MonetaryAmount,
    val nonLendingAmount: MonetaryAmount,
    val revenue: MonetaryAmount,
    val successRate: Double
)