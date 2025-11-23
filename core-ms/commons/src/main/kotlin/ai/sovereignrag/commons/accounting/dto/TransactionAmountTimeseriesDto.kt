package ai.sovereignrag.commons.accounting.dto

import java.time.Instant
import javax.money.MonetaryAmount

data class TransactionAmountDataPoint(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalAmount: MonetaryAmount,
    val totalTransactions: Long
)

data class TransactionAmountTimeseriesDto(
    val timeseries: List<TransactionAmountDataPoint>,
    val intervalType: String
)