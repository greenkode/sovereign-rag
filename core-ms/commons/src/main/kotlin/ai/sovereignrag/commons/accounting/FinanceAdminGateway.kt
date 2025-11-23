package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.accounting.dto.ChartAccountsDto
import ai.sovereignrag.commons.accounting.dto.ChartCurrencyDto

interface FinanceAdminGateway {

    fun getChartAccounts(
        currency: String,
        accountId: String,
        page: Int = 0,
        pageSize: Int = 10
    ): ChartAccountsDto

    fun getChartCurrencies(): List<ChartCurrencyDto>
}