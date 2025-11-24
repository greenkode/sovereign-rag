package ai.sovereignrag.accounting.gateway.api.response

data class AdminChartAccountsResponse(
    val accounts: List<ChartAccount>,
    val pagination: Pagination
)

data class ChartAccount(
    val id: String,
    val code: String,
    val name: String,
    val type: String,
    val level: Int,
    val balance: Double,
    val children: List<ChartAccount>?,
    val childrenPagination: Pagination?,
    val composite: Boolean
)

data class Pagination(
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)