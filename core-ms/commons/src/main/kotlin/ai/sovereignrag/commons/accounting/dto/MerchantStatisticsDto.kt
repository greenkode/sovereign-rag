package ai.sovereignrag.commons.accounting.dto

data class MerchantStatisticsDto(
    val totalMerchants: Int,
    val activeMerchants: Int,
    val prevActiveMerchants: Int
)