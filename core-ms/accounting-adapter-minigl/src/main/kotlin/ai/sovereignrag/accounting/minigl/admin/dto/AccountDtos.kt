package ai.sovereignrag.accounting.minigl.admin.dto

data class AccountResult(
    val id: String,
    val name: String,
    val type: String,
    val level: Int,
    val balance: Double,
    val isComposite: Boolean,
    val children: List<AccountResult>? = null,
    val childrenPagination: ChildrenPagination? = null
)

data class ChildrenPagination(
    val currentPage: Int,
    val pageSize: Int,
    val totalChildren: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)

data class AccountsResponse(
    val accounts: List<AccountResult>,
    val pagination: Pagination
)

data class Pagination(
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)