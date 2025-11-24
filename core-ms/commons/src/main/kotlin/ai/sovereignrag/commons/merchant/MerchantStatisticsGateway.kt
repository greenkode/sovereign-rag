package ai.sovereignrag.commons.merchant

import ai.sovereignrag.commons.accounting.dto.MerchantTransactionStatisticsDto
import ai.sovereignrag.commons.accounting.dto.TransactionCountDto
import ai.sovereignrag.commons.accounting.dto.MerchantStatisticsDto
import ai.sovereignrag.commons.accounting.dto.MerchantPortalOverviewDto
import ai.sovereignrag.commons.accounting.dto.TransactionAmountTimeseriesDto
import ai.sovereignrag.commons.accounting.dto.SuccessRateDto
import ai.sovereignrag.commons.accounting.dto.MerchantTransactionAnalyticsDto
import java.time.Instant
import java.util.UUID

interface MerchantStatisticsGateway {
    
    fun getMerchantTransactionStatistics(startDate: Instant, endDate: Instant): List<MerchantTransactionStatisticsDto>
    
    fun getTransactionCountsByMerchant(): List<TransactionCountDto>
    
    fun getMerchantStatistics(startDate: Instant, endDate: Instant): MerchantStatisticsDto
    
    fun getMerchantPortalOverviewStatistics(merchantId: UUID, startDate: Instant, endDate: Instant): MerchantPortalOverviewDto?
    
    fun getMerchantTransactionAmountTimeseries(merchantId: UUID, startDate: Instant, endDate: Instant): TransactionAmountTimeseriesDto
    
    fun getMerchantSuccessRate(merchantId: UUID, startDate: Instant, endDate: Instant): SuccessRateDto
    
    fun getMerchantTransactionAnalytics(merchantId: UUID, startDate: Instant, endDate: Instant): MerchantTransactionAnalyticsDto
}