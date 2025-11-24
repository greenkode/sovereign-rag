package ai.sovereignrag.accounting.gateway

import ai.sovereignrag.accounting.gateway.api.response.AdminChartAccountsResponse
import ai.sovereignrag.accounting.gateway.api.response.ChartAccount
import ai.sovereignrag.accounting.gateway.api.response.ChartCurrencyResponse
import ai.sovereignrag.accounting.gateway.api.response.Pagination
import ai.sovereignrag.commons.accounting.MiniglChartAdminGateway
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.stereotype.Service

@Service
class AccountingAdminGateway(
    private val chartAdminGateway: MiniglChartAdminGateway
) {

    private val log = logger {}

    @LogExecutionTime
    fun getChartAccounts(
        currency: String,
        accountId: String,
        page: Int = 0,
        pageSize: Int = 10
    ): AdminChartAccountsResponse {
        val result = chartAdminGateway.getChartAccounts(currency, accountId, page, pageSize)

        return AdminChartAccountsResponse(
            accounts = result.accounts.map { account ->
                ChartAccount(
                    id = account.id,
                    code = account.code,
                    name = account.name,
                    type = account.type,
                    level = account.level,
                    balance = account.balance,
                    children = account.children?.map { child ->
                        ChartAccount(
                            id = child.id,
                            code = child.code,
                            name = child.name,
                            type = child.type,
                            level = child.level,
                            balance = child.balance,
                            children = null,
                            childrenPagination = child.childrenPagination?.let { childPagination ->
                                Pagination(
                                    currentPage = childPagination.currentPage,
                                    pageSize = childPagination.pageSize,
                                    totalItems = childPagination.totalItems,
                                    totalPages = childPagination.totalPages,
                                    hasNextPage = childPagination.hasNextPage,
                                    hasPreviousPage = childPagination.hasPreviousPage
                                )
                            },
                            composite = child.composite
                        )
                    },
                    childrenPagination = account.childrenPagination?.let { childPagination ->
                        Pagination(
                            currentPage = childPagination.currentPage,
                            pageSize = childPagination.pageSize,
                            totalItems = childPagination.totalItems,
                            totalPages = childPagination.totalPages,
                            hasNextPage = childPagination.hasNextPage,
                            hasPreviousPage = childPagination.hasPreviousPage
                        )
                    },
                    composite = account.composite
                )
            },
            pagination = Pagination(
                currentPage = result.pagination.currentPage,
                pageSize = result.pagination.pageSize,
                totalItems = result.pagination.totalItems,
                totalPages = result.pagination.totalPages,
                hasNextPage = result.pagination.hasNextPage,
                hasPreviousPage = result.pagination.hasPreviousPage
            )
        )
    }

    @LogExecutionTime
    fun getChartCurrencies(): List<ChartCurrencyResponse> {
        val result = chartAdminGateway.getChartCurrencies()

        return result.map { currency ->
            ChartCurrencyResponse(
                id = currency.id,
                code = currency.code,
                name = currency.name
            )
        }
    }
}
