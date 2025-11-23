package ai.sovereignrag.commons.accounting.dto

import java.util.UUID
import javax.money.MonetaryAmount

data class MerchantPortalOverviewDto(
    val merchantId: UUID,
    val totalAmountVended: MonetaryAmount,
    val totalTransactions: Long,
    val successfulTransactionsValue: MonetaryAmount,
    val successfulTransactionsVolume: Long,
    val pendingTransactionsValue: MonetaryAmount,
    val pendingTransactionsVolume: Long,
    val failedTransactionsValue: MonetaryAmount,
    val failedTransactionsVolume: Long
)