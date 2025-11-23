package ai.sovereignrag.commons.accounting.dto

import javax.money.MonetaryAmount

data class DashboardOverviewStatisticsDto(
    // Current period values
    val totalRevenue: MonetaryAmount,
    val totalTransactions: Int,
    val totalPendingTransactions: Int,
    val totalPendingValue: MonetaryAmount,
    val totalActiveMerchants: Int,
    val totalActiveVendors: Int,
    val totalAmount: MonetaryAmount,
    val totalFees: MonetaryAmount,
    val totalCommission: MonetaryAmount,
    val avgVendingTimeSeconds: Double,
    // Current period loan metrics
    val loanRevenue: MonetaryAmount,
    val loanTransactions: Int,
    val nonLoanRevenue: MonetaryAmount,
    val nonLoanTransactions: Int,
    // Due amount (pending loan transactions)
    val dueAmount: MonetaryAmount,
    // Current period transaction status breakdown
    val successfulTransactionsValue: MonetaryAmount,
    val successfulTransactionsVolume: Int,
    val failedTransactionsValue: MonetaryAmount,
    val failedTransactionsVolume: Int,
    // Previous period values
    val prevTotalRevenue: MonetaryAmount,
    val prevTotalTransactions: Int,
    val prevTotalPendingTransactions: Int,
    val prevTotalPendingValue: MonetaryAmount,
    val prevTotalActiveMerchants: Int,
    val prevTotalActiveVendors: Int,
    val prevTotalAmount: MonetaryAmount,
    val prevTotalFees: MonetaryAmount,
    val prevTotalCommission: MonetaryAmount,
    val prevAvgVendingTimeSeconds: Double,
    // Previous period loan metrics
    val prevLoanRevenue: MonetaryAmount,
    val prevLoanTransactions: Int,
    val prevNonLoanRevenue: MonetaryAmount,
    val prevNonLoanTransactions: Int,
    val prevDueAmount: MonetaryAmount,
    // Previous period transaction status breakdown
    val prevSuccessfulTransactionsValue: MonetaryAmount,
    val prevSuccessfulTransactionsVolume: Int,
    val prevFailedTransactionsValue: MonetaryAmount,
    val prevFailedTransactionsVolume: Int,
    val dataSource: String
)