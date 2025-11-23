package ai.sovereignrag.commons.accounting.dto

import org.javamoney.moneta.Money


data class TransactionMetricsDto(
    // Current period metrics
    val totalVolume: Int,
    val totalValue: Money,
    val completedCount: Int,
    val failedCount: Int,
    val reversedCount: Int,
    
    // Previous period metrics
    val prevTotalVolume: Int,
    val prevTotalValue: Money,
    val prevCompletedCount: Int,
    val prevFailedCount: Int,
    val prevReversedCount: Int,
    
    // Metadata
    val dataSource: String
)