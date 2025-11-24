package ai.sovereignrag.accounting.minigl.gateway

import ai.sovereignrag.accounting.entity.CompositeAccountEntity
import ai.sovereignrag.accounting.entity.GLAccountEntity
import ai.sovereignrag.accounting.minigl.account.dao.MiniglAccountRepository
import ai.sovereignrag.accounting.minigl.account.service.MiniglAccountService
import ai.sovereignrag.accounting.minigl.currency.dao.MiniglCurrencyRepository
import ai.sovereignrag.accounting.minigl.journal.JournalService
import ai.sovereignrag.commons.accounting.ChartAccountDto
import ai.sovereignrag.commons.accounting.ChartAccountsResponseDto
import ai.sovereignrag.commons.accounting.ChartCurrencyDto
import ai.sovereignrag.commons.accounting.MiniglChartAdminGateway
import ai.sovereignrag.commons.accounting.PaginationDto
import ai.sovereignrag.commons.exception.RecordNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MiniglChartAdminGatewayAdapter(
    private val accountService: MiniglAccountService,
    private val accountRepository: MiniglAccountRepository,
    private val journalService: JournalService,
    private val currencyRepository: MiniglCurrencyRepository
) : MiniglChartAdminGateway {

    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    override fun getChartAccounts(
        currency: String,
        accountId: String,
        page: Int,
        pageSize: Int
    ): ChartAccountsResponseDto {
        val chart = accountService.getDefaultChart()
            ?: throw RecordNotFoundException("Default chart not found")

        val account = if (accountId == chart.code) chart else
            accountRepository.getAccountByCode(chart, accountId)
                ?: throw RecordNotFoundException("Account not found: $accountId")

        val journal = journalService.getJournal(chart)
            ?: throw RecordNotFoundException("Journal not found for chart: ${chart.description}")

        if (!account.isCompositeAccount) {
            val currencyEntity = currencyRepository.getCurrencyByCode(currency)
                ?: throw RecordNotFoundException("Unable to find currency: $currency")

            val balance = accountRepository.getBalance(journal, account, currencyEntity).number.doubleValueExact()

            return ChartAccountsResponseDto(
                accounts = listOf(
                    ChartAccountDto(
                        id = account.code,
                        name = getFormattedAccountName(account),
                        code = account.code,
                        type = if (account.isDebit) "DEBIT" else "CREDIT",
                        level = getAccountLevel(account),
                        balance = balance,
                        composite = account.isCompositeAccount,
                        children = null,
                        childrenPagination = null
                    )
                ),
                pagination = PaginationDto(
                    currentPage = page,
                    pageSize = pageSize,
                    totalItems = 1,
                    totalPages = 1,
                    hasNextPage = false,
                    hasPreviousPage = page > 0
                )
            )
        }

        val (childAccounts, totalCount) = accountRepository.getChildrenAccountsPaginated(
            account as CompositeAccountEntity,
            page,
            pageSize,
            currency
        )

        val accountsToProcess = if (chart.currencyCode == currency) {
            listOf(chart) + childAccounts
        } else {
            childAccounts
        }

        val currencyEntity = currencyRepository.getCurrencyByCode(currency)
            ?: throw RecordNotFoundException("Unable to find currency: $currency")

        val accountDtos = accountsToProcess.map { acc ->
            val balance = accountRepository.getBalance(journal, acc, currencyEntity).number.doubleValueExact()
            ChartAccountDto(
                id = acc.code,
                name = getFormattedAccountName(acc),
                code = acc.code,
                type = if (acc.isDebit) "DEBIT" else "CREDIT",
                level = getAccountLevel(acc),
                balance = balance,
                composite = acc.isCompositeAccount,
                children = null,
                childrenPagination = null
            )
        }.sortedBy { it.balance }

        return ChartAccountsResponseDto(
            accounts = accountDtos,
            pagination = PaginationDto(
                currentPage = page,
                pageSize = pageSize,
                totalItems = totalCount.toInt(),
                totalPages = if (pageSize > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 1,
                hasNextPage = page < ((totalCount + pageSize - 1) / pageSize).toInt() - 1,
                hasPreviousPage = page > 0
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getChartCurrencies(): List<ChartCurrencyDto> {
        val chart = accountService.getDefaultChart()
            ?: throw RecordNotFoundException("Default chart not found")

        val currencyCodes = accountRepository.getDistinctCurrencyCodesInChart(chart)

        val currencies = if (currencyCodes.isNotEmpty()) {
            currencyRepository.getCurrenciesByCodes(currencyCodes)
        } else {
            emptyList()
        }

        return currencies.map { currency ->
            ChartCurrencyDto(
                id = currency.id,
                code = currency.name,
                name = currency.name
            )
        }.sortedBy { it.code }
    }

    private fun getAccountLevel(account: GLAccountEntity): Int {
        var level = 0
        var current = account.parent
        while (current != null && current != account.root) {
            level++
            current = current.parent
        }
        return level
    }

    private fun GLAccountEntity.getTag(name: String): String? {
        return try {
            tags?.toString()?.split(",")?.firstOrNull { it.startsWith(name) }?.split(":")?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFormattedAccountName(account: GLAccountEntity): String {
        return if (!account.isCompositeAccount) {
            val accountName = account.getTag("account_name") ?: account.description
            val type = account.getTag("type")
            if (type != null) "$accountName ($type)" else accountName
        } else {
            account.description
        }
    }
}
