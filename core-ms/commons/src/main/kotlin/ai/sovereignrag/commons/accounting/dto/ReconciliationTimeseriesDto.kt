package ai.sovereignrag.commons.accounting.dto

import java.time.Instant

data class ReconciliationTimeseriesDto(
    val timeseries: List<ReconciliationDataPoint>,
    val intervalType: String? = null
)

data class ReconciliationDataPoint(
    val periodStart: Instant,
    val periodEnd: Instant,
    val avgReconciliationTimeSeconds: Double
)