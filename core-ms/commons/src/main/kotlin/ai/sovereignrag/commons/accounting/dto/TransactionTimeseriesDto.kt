package ai.sovereignrag.commons.accounting.dto

import java.time.Instant

data class TransactionTimeseriesDto(
    val timeseries: List<TransactionTimeseriesDataPoint>,
    val intervalType: String? = null
)

data class TransactionTimeseriesDataPoint(
    val periodStart: Instant,
    val periodEnd: Instant,
    val successfulCount: Int,
    val successfulValue: Double,
    val failedCount: Int,
    val failedValue: Double
)