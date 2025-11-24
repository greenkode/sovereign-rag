package ai.sovereignrag.commons.vendor.dto

import java.time.LocalDate
import java.util.UUID
import javax.money.MonetaryAmount

data class VendorTransactionStatisticsDto(
    val vendorId: UUID,
    val vendorName: String,
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
    val commission: MonetaryAmount,
    val successRate: Double,
    val avgProcessingTimeSeconds: Double,
    val uptimePercentage: Double
)

data class VendorStatisticsDto(
    val totalVendors: Int,
    val activeVendors: Int,
    val prevActiveVendors: Int
)

data class VendorFinancialMetricsDto(
    val currentRevenue: MonetaryAmount,
    val currentCommission: MonetaryAmount,
    val currentLendingVolume: Long,
    val currentLendingValue: MonetaryAmount,
    val currentNonLendingVolume: Long,
    val currentNonLendingValue: MonetaryAmount,
    val previousRevenue: MonetaryAmount,
    val previousCommission: MonetaryAmount,
    val previousLendingVolume: Long,
    val previousLendingValue: MonetaryAmount,
    val previousNonLendingVolume: Long,
    val previousNonLendingValue: MonetaryAmount,
    val walletBalance: MonetaryAmount
)

data class VendorReconciliationTimeDto(
    val averageReconciliationTimeSeconds: Double,
    val minReconciliationTimeSeconds: Double,
    val maxReconciliationTimeSeconds: Double,
    val totalTransactions: Long,
    val previousAverageReconciliationTimeSeconds: Double,
    val previousMinReconciliationTimeSeconds: Double,
    val previousMaxReconciliationTimeSeconds: Double,
    val previousTotalTransactions: Long
)

data class VendorReconciliationTimeSeriesDto(
    val date: LocalDate,
    val averageReconciliationTimeSeconds: Double,
    val minReconciliationTimeSeconds: Double,
    val maxReconciliationTimeSeconds: Double,
    val transactionCount: Long
)

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
    val commission: MonetaryAmount,
    val successRate: Double,
    val avgProcessingTimeSeconds: Double,
    val uptimePercentage: Double
)