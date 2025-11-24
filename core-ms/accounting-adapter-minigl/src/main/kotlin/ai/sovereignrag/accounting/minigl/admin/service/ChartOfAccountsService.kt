package ai.sovereignrag.accounting.minigl.admin.service

import ai.sovereignrag.accounting.minigl.account.dao.LatestBalanceSnapshotRepository
import ai.sovereignrag.accounting.minigl.account.dao.MiniglAccountRepository
import ai.sovereignrag.accounting.minigl.admin.dto.AccountResult
import ai.sovereignrag.accounting.minigl.admin.dto.AccountsResponse
import ai.sovereignrag.accounting.minigl.admin.dto.Pagination
import ai.sovereignrag.accounting.minigl.currency.dao.MiniglCurrencyRepository
import ai.sovereignrag.accounting.minigl.exception.RecordNotFoundException
import ai.sovereignrag.accounting.minigl.journal.JournalService
import org.springframework.stereotype.Service
import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLAccountEntity as Account

@Service
class ChartOfAccountsService(
    private val miniglAccountRepository: MiniglAccountRepository,
    private val journalService: JournalService,
    private val latestBalanceSnapshotRepository: LatestBalanceSnapshotRepository,
    private val miniglCurrencyRepository: MiniglCurrencyRepository
) {

    fun getAccountByIdAndCurrency(
        chartDescription: String,
        id: String,
        currency: String,
        page: Int,
        pageSize: Int
    ): AccountsResponse {
        // Get the chart of accounts
        val chart = miniglAccountRepository.getChartByDescription(chartDescription)
            ?: throw IllegalArgumentException("Chart not found: $chartDescription")

        val account = if (id == chart.code) chart else
            miniglAccountRepository.getAccountByCode(chart, id)
                ?: throw IllegalArgumentException("Account not found: $id")

        // Get the journal for this chart
        val journal = journalService.getJournal(chart)
            ?: throw IllegalArgumentException("Journal not found for chart: $chartDescription")

        if (!account.isCompositeAccount) {
            val currencyEntity = miniglCurrencyRepository.getCurrencyByCode(currency)
                ?: throw RecordNotFoundException("Unable to find currency: $currency")

            return AccountsResponse(
                accounts = listOf(
                    AccountResult(
                        id = account.code,
                        name = getFormattedAccountName(account),
                        type = if (account.isDebit) "DEBIT" else "CREDIT",
                        level = getAccountLevel(account),
                        balance = miniglAccountRepository.getBalance(
                            journal,
                            account,
                            currencyEntity
                        ).number.doubleValueExact(),
                        isComposite = account.isCompositeAccount,
                        children = null,
                        childrenPagination = null
                    )
                ),
                pagination = Pagination(
                    currentPage = page,
                    pageSize = pageSize,
                    totalItems = 1,
                    totalPages = 1,
                    hasNextPage = false,
                    hasPreviousPage = page > 0
                )
            )
        }

        // Get only direct children of the chart with pagination and currency filter
        val (childAccounts, totalCount) = miniglAccountRepository.getChildrenAccountsPaginated(
            account as CompositeAccount,
            page,
            pageSize,
            currency
        )

        // Include the chart itself if it matches the currency
        val accountsToProcess = if (chart.currencyCode == currency) {
            listOf(chart) + childAccounts
        } else {
            childAccounts
        }

        val currency = miniglCurrencyRepository.getCurrencyByCode(currency)
            ?: throw RecordNotFoundException("Unable to find currency: $currency")

        // Get account balances efficiently using batch repository call
        val balances = latestBalanceSnapshotRepository.getMultipleAccountBalances(
            journal,
            accountsToProcess,
            layers = ShortArray(1) { currency.id.toShort() })

        // Convert to DTOs with balances
        val accountDtos = accountsToProcess.map { account ->
            val balance = balances[account.id]?.toDouble() ?: 0.0
            AccountResult(
                id = account.code,
                name = getFormattedAccountName(account),
                type = if (account.isDebit) "DEBIT" else "CREDIT",
                level = getAccountLevel(account),
                balance = balance,
                isComposite = account.isCompositeAccount,
                children = null,
                childrenPagination = null
            )
        }.sortedBy { it.balance }

        return AccountsResponse(
            accounts = accountDtos,
            pagination = Pagination(
                currentPage = page,
                pageSize = pageSize,
                totalItems = totalCount.toInt(),
                totalPages = if (pageSize > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 1,
                hasNextPage = page < ((totalCount + pageSize - 1) / pageSize).toInt() - 1,
                hasPreviousPage = page > 0
            )
        )
    }

    private fun getAccountLevel(account: Account): Int {
        var level = 0
        var current = account.parent
        while (current != null && current != account.root) {
            level++
            current = current.parent
        }
        return level
    }

    fun getAccountChildrenByCurrency(
        chartDescription: String,
        accountCode: String,
        currency: String,
        page: Int,
        pageSize: Int
    ): AccountsResponse {
        // Get the chart of accounts
        val chart = miniglAccountRepository.getChartByDescription(chartDescription)
            ?: throw IllegalArgumentException("Chart not found: $chartDescription")

        // Get the specific account
        val account = miniglAccountRepository.getCompositeAccountByChartAndCode(chart, accountCode)
            ?: throw IllegalArgumentException("Account not found: $accountCode")

        // Get the journal for this chart
        val journal = journalService.getJournal(chart)
            ?: throw IllegalArgumentException("Journal not found for chart: $chartDescription")

        if (!account.isCompositeAccount) {

            val currencyEntity = miniglCurrencyRepository.getCurrencyByCode(currency)
                ?: throw IllegalArgumentException("Currency not found: $currency")

            return AccountsResponse(
                accounts = listOf(
                    AccountResult(
                        account.code,
                        getFormattedAccountName(account),
                        account.currencyCode,
                        account.level,
                        miniglAccountRepository.getBalance(journal, account, currencyEntity).number.doubleValueExact(),
                        account.isCompositeAccount,
                        listOf(),
                    )
                ),
                pagination = Pagination(
                    currentPage = page,
                    pageSize = pageSize,
                    totalItems = 0,
                    totalPages = 1,
                    hasNextPage = false,
                    hasPreviousPage = page > 0
                )
            )
        }

        // Get paginated children of the specific account with currency filter
        val (childAccounts, totalCount) = miniglAccountRepository.getChildrenAccountsPaginated(
            account,
            page,
            pageSize,
            currency
        )

        val currencyEntity = miniglCurrencyRepository.getCurrencyByCode(currency)
            ?: throw RecordNotFoundException("Unable to find currency: $currency")

        // Get account balances efficiently using batch repository call
        val balances = latestBalanceSnapshotRepository.getMultipleAccountBalances(
            journal,
            childAccounts,
            layers = ShortArray(1) { currencyEntity.id.toShort() })

        // Convert to DTOs with balances
        val accountDtos = childAccounts.map { childAccount ->
            val balance = balances[childAccount.id]?.toDouble() ?: 0.0
            AccountResult(
                id = childAccount.code,
                name = getFormattedAccountName(childAccount),
                type = if (childAccount.isDebit) "DEBIT" else "CREDIT",
                level = getAccountLevel(childAccount),
                balance = balance,
                isComposite = childAccount.isCompositeAccount,
                children = null,
                childrenPagination = null
            )
        }.sortedBy { it.id }

        return AccountsResponse(
            accounts = accountDtos,
            pagination = Pagination(
                currentPage = page,
                pageSize = pageSize,
                totalItems = totalCount.toInt(),
                totalPages = if (pageSize > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 1,
                hasNextPage = page < ((totalCount + pageSize - 1) / pageSize).toInt() - 1,
                hasPreviousPage = page > 0
            )
        )
    }

    /**
     * Get immediate children of a composite account with pagination and currency filter
     * @param compositeAccount The composite account to get children for
     * @param currency The currency to filter by
     * @param page The page number (0-based)
     * @param pageSize The number of items per page
     * @return PaginatedAccountHierarchy containing the account and its immediate children
     */
    fun getAccountChildren(
        compositeAccount: CompositeAccount,
        currency: String,
        page: Int,
        pageSize: Int
    ): PaginatedAccountHierarchy {
        val (children, totalCount) = miniglAccountRepository.getChildrenAccountsPaginated(
            compositeAccount,
            page,
            pageSize,
            currency
        )

        val childHierarchies = children.map { child ->
            when (child) {
                is CompositeAccount -> AccountHierarchy(
                    code = child.code,
                    description = child.description,
                    type = child.type,
                    currencyCode = child.currencyCode,
                    isComposite = true,
                    children = emptyList() // Only immediate children, no recursion
                )

                is FinalAccount -> AccountHierarchy(
                    code = child.code,
                    description = getFormattedAccountName(child),
                    type = child.type,
                    currencyCode = child.currencyCode,
                    isComposite = false,
                    children = emptyList()
                )

                else -> throw IllegalStateException("Unexpected account type: ${child::class.java}")
            }
        }

        return PaginatedAccountHierarchy(
            account = AccountHierarchy(
                code = compositeAccount.code,
                description = compositeAccount.description,
                type = compositeAccount.type,
                currencyCode = compositeAccount.currencyCode,
                isComposite = true,
                children = childHierarchies
            ),
            page = page,
            pageSize = pageSize,
            totalElements = totalCount,
            totalPages = if (pageSize > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        )
    }

    fun Account.getTag(name: String): String? {
        return try {
            tags?.toString()?.split(",")?.firstOrNull { it.startsWith(name) }?.split(":")?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFormattedAccountName(account: Account): String {
        return if (!account.isCompositeAccount) {
            val accountName = account.getTag("account_name") ?: account.description
            val type = account.getTag("type")
            if (type != null) "$accountName ($type)" else accountName
        } else {
            account.description
        }
    }
}

/**
 * Data class representing an account hierarchy node
 */
data class AccountHierarchy(
    val code: String,
    val description: String,
    val type: Int,
    val currencyCode: String?,
    val isComposite: Boolean,
    val children: List<AccountHierarchy>
)

/**
 * Data class representing a paginated account hierarchy response
 */
data class PaginatedAccountHierarchy(
    val account: AccountHierarchy,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

/**
 * Data class representing a currency response
 */
data class CurrencyResponse(
    val id: String,
    val code: String,
    val name: String
)