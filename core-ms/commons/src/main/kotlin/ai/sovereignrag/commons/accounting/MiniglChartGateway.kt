package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.coa.ChartOfAccountsRequest


interface MiniglChartGateway {
    
    fun getChartDetails(): ChartDetailsResponseDto
    
    fun createNewChart(chart: ChartOfAccountsRequest)
}

data class ChartDetailsResponseDto(
    val description: String,
    val code: String
)