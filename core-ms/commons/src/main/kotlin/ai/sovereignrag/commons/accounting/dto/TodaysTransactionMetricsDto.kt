package ai.sovereignrag.commons.accounting.dto

import javax.money.MonetaryAmount

data class TodaysTransactionMetricsDto(
    val transactionValue: MonetaryAmount,
    val transactionVolume: Int,
    val commission: MonetaryAmount
)