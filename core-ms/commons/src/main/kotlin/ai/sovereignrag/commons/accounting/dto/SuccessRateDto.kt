package ai.sovereignrag.commons.accounting.dto

import java.util.UUID

data class SuccessRateDto(
    val merchantId: UUID,
    val totalSuccessfulTransactions: Long,
    val totalFailedTransactions: Long,
    val totalTransactions: Long,
    val successPercentage: Double
)