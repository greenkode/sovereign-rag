package ai.sovereignrag.accounting.service

import ai.sovereignrag.accounting.gateway.AccountingAdminGateway as InternalAccountingAdminGateway
import ai.sovereignrag.commons.accounting.FinanceAdminGateway
import ai.sovereignrag.commons.accounting.dto.ChartAccountDto
import ai.sovereignrag.commons.accounting.dto.ChartAccountsDto
import ai.sovereignrag.commons.accounting.dto.ChartCurrencyDto
import ai.sovereignrag.commons.accounting.dto.PaginationDto
import org.springframework.stereotype.Service

@Service
class AccountingAdminService(
    private val accountingAdminGateway: InternalAccountingAdminGateway
) : FinanceAdminGateway {

    override fun getChartAccounts(
        currency: String,
        accountId: String,
        page: Int,
        pageSize: Int
    ): ChartAccountsDto {

        val response = accountingAdminGateway.getChartAccounts(currency, accountId, page, pageSize)

        return ChartAccountsDto(
            accounts = response.accounts.map { account ->
                ChartAccountDto(
                    id = account.id,
                    code = account.code,
                    name = account.name,
                    type = account.type,
                    level = account.level,
                    balance = account.balance,
                    children = account.children?.map { child ->
                        ChartAccountDto(
                            id = child.id,
                            code = child.code,
                            name = child.name,
                            type = child.type,
                            level = child.level,
                            balance = child.balance,
                            children = null,
                            childrenPagination = null,
                            composite = child.composite
                        )
                    },
                    childrenPagination = account.childrenPagination?.let { pagination ->
                        PaginationDto(
                            currentPage = pagination.currentPage,
                            pageSize = pagination.pageSize,
                            totalItems = pagination.totalItems,
                            totalPages = pagination.totalPages,
                            hasNextPage = pagination.hasNextPage,
                            hasPreviousPage = pagination.hasPreviousPage
                        )
                    },
                    composite = account.composite
                )
            },
            pagination = PaginationDto(
                currentPage = response.pagination.currentPage,
                pageSize = response.pagination.pageSize,
                totalItems = response.pagination.totalItems,
                totalPages = response.pagination.totalPages,
                hasNextPage = response.pagination.hasNextPage,
                hasPreviousPage = response.pagination.hasPreviousPage
            )
        )
    }

    override fun getChartCurrencies(): List<ChartCurrencyDto> {
        val currencies = accountingAdminGateway.getChartCurrencies()
        
        return currencies.map { currency ->
            ChartCurrencyDto(
                id = currency.id,
                code = currency.code,
                name = currency.name
            )
        }
    }
}