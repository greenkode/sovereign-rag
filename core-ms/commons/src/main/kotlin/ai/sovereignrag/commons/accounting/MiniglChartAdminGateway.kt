package ai.sovereignrag.commons.accounting

interface MiniglChartAdminGateway {

    fun getChartAccounts(
        currency: String,
        accountId: String,
        page: Int,
        pageSize: Int
    ): ChartAccountsResponseDto

    fun getChartCurrencies(): List<ChartCurrencyDto>
}

data class ChartAccountsResponseDto(
    val accounts: List<ChartAccountDto>,
    val pagination: PaginationDto
)

data class ChartAccountDto(
    val id: String,
    val code: String,
    val name: String,
    val type: String,
    val level: Int,
    val balance: Double,
    val children: List<ChartAccountDto>?,
    val childrenPagination: PaginationDto?,
    val composite: Boolean
)

data class PaginationDto(
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)

data class ChartCurrencyDto(
    val id: String,
    val code: String,
    val name: String
)
