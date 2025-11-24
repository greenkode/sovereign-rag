package ai.sovereignrag.commons.vendor

import ai.sovereignrag.commons.vendor.dto.VendorFinancialMetricsDto
import ai.sovereignrag.commons.vendor.dto.VendorReconciliationTimeDto
import ai.sovereignrag.commons.vendor.dto.MerchantTransactionStatisticsDto
import ai.sovereignrag.commons.vendor.dto.VendorReconciliationTimeSeriesDto
import ai.sovereignrag.commons.vendor.dto.VendorStatisticsDto
import ai.sovereignrag.commons.vendor.dto.VendorTransactionStatisticsDto
import java.time.Instant
import java.util.UUID

interface VendorStatisticsGateway {
    
    fun getVendorTransactionStatistics(startDate: Instant, endDate: Instant): List<VendorTransactionStatisticsDto>
    
    fun getVendorStatistics(startDate: Instant, endDate: Instant): VendorStatisticsDto

    fun getVendorDetailsById(vendorId: UUID): VendorTransactionStatisticsDto?
    
    fun getVendorFinancialMetrics(startDate: Instant, endDate: Instant, vendorId: UUID? = null): VendorFinancialMetricsDto
    
    fun getVendorReconciliationTime(startDate: Instant, endDate: Instant, vendorId: UUID? = null): VendorReconciliationTimeDto
    
    fun getVendorReconciliationTimeSeries(startDate: Instant, endDate: Instant, vendorId: UUID? = null): List<VendorReconciliationTimeSeriesDto>
    
    fun getVendorMerchantTransactionStatistics(vendorId: UUID, startDate: Instant, endDate: Instant): List<MerchantTransactionStatisticsDto>
}