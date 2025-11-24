package ai.sovereignrag.accounting.minigl.gateway

import ai.sovereignrag.accounting.config.ChartOfAccountsRequest
import ai.sovereignrag.accounting.minigl.account.service.CoaImportService
import ai.sovereignrag.accounting.minigl.account.service.MiniglAccountService
import ai.sovereignrag.commons.accounting.ChartDetailsResponseDto
import ai.sovereignrag.commons.accounting.MiniglChartGateway
import ai.sovereignrag.commons.exception.AccountServiceException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import org.springframework.stereotype.Service

@Service
class MiniglChartGatewayAdapter(
    private val accountService: MiniglAccountService,
    private val coaImportService: CoaImportService
) : MiniglChartGateway {

    override fun getChartDetails(): ChartDetailsResponseDto {
        val chart = accountService.getDefaultChart()
            ?: throw RecordNotFoundException("Default chart not found")
        
        return ChartDetailsResponseDto(
            chart.description,
            chart.code
        )
    }

    override fun createNewChart(chart: ChartOfAccountsRequest) {

        coaImportService.import(chart)
            ?: throw AccountServiceException("Unable to create new currency chart for chart: $chart")
    }
}