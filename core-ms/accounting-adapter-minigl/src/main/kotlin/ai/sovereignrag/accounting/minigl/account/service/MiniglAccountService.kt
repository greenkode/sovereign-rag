package ai.sovereignrag.accounting.minigl.account.service

import ai.sovereignrag.accounting.Tags
import ai.sovereignrag.accounting.minigl.account.dao.LatestBalanceSnapshotRepository
import ai.sovereignrag.accounting.minigl.account.dao.MiniglAccountRepository
import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import ai.sovereignrag.accounting.minigl.currency.dao.MiniglCurrencyRepository
import ai.sovereignrag.accounting.minigl.journal.JournalService
import ai.sovereignrag.commons.cache.CacheNames
import org.javamoney.moneta.Money
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount
import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.CurrencyEntity as Currency
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLAccountEntity as Account

@Service
class MiniglAccountService(
    private val miniglAccountRepository: MiniglAccountRepository,
    private val journalService: JournalService,
    private val miniglCurrencyRepository: MiniglCurrencyRepository,
    private val latestBalanceSnapshotRepository: LatestBalanceSnapshotRepository,
    private val miniglTransactionRepository: ai.sovereignrag.accounting.minigl.transaction.dao.MiniglTransactionRepository,
) {


    fun createAccount(
        chart: CompositeAccount,
        parentCode: String,
        currency: String,
        desc: String,
        metadata: Map<String, String>,
        padding: Int,
        finalAccount: Boolean,
        type: String?
    ): Account {

        val account =
            miniglAccountRepository.getAccountWithChartDescriptionAndCurrency(chart, desc, currency) ?: run {

                val parentAccount =
                    miniglAccountRepository.getCompositeAccountByChartAndCode(chart, parentCode)
                        ?: throw RuntimeException(
                            "Account with code: $parentCode not found"
                        )

                val account =
                    createSubAccount(finalAccount, parentAccount, padding, desc, currency, chart, metadata, type)

                if (finalAccount) {
                    account
                } else
                    miniglAccountRepository.getCompositeAccountByChartAndCode(chart, account.code)
            }

        if (finalAccount) {

            miniglAccountRepository.getFinalAccountByChartAndDescription(
                chart,
                "${MiniglConstants.BRIDGE_LIABILITIES}-$desc"
            ) ?: run {

                miniglAccountRepository.getCompositeAccountByChartAndDescription(
                    chart,
                    MiniglConstants.BRIDGE_LIABILITIES
                )
                    ?.let {
                        createSubAccount(
                            true,
                            it,
                            padding,
                            "${MiniglConstants.BRIDGE_LIABILITIES}-$desc",
                            currency,
                            chart,
                            metadata,
                            type
                        )
                    }
            }

            miniglAccountRepository.getFinalAccountByChartAndDescription(
                chart,
                "${MiniglConstants.BRIDGE_ASSETS}-$desc"
            ) ?: run {
                miniglAccountRepository.getCompositeAccountByChartAndDescription(chart, MiniglConstants.BRIDGE_ASSETS)
                    ?.let {
                        createSubAccount(
                            true,
                            it,
                            padding,
                            "${MiniglConstants.BRIDGE_ASSETS}-$desc",
                            currency,
                            chart,
                            metadata,
                            type
                        )
                    }
            }
        }

        return account
            ?: throw RuntimeException("Failed to create account for currency: $currency and chart ${chart.code}")
    }

    private fun createSubAccount(
        finalAccount: Boolean,
        parentAccount: CompositeAccount,
        padding: Int,
        desc: String,
        currency: String,
        chart: CompositeAccount,
        metadata: Map<String, String>,
        type: String? = null
    ): Account {
        val account = if (finalAccount) FinalAccount() else CompositeAccount()
        account.code = generateId(parentAccount, padding)
        account.description = desc
        account.created = Instant.now()
        account.currencyCode = currency
        account.type = parentAccount.type
        account.root = chart
        account.parent = parentAccount

        val allTags = mutableMapOf<String, String>().apply {
            putAll(metadata)
            type?.let { put("type", it) }
        }

        if (allTags.isNotEmpty())
            account.tags = Tags(allTags.entries.joinToString(",") { "${it.key}:${it.value}" })

        miniglAccountRepository.addAccountFast(parentAccount, account)

        return account
    }

    @Cacheable(CacheNames.ACCOUNT, key = "#chart.id + '_' + #code")
    fun getAccountByChartAndCode(chart: CompositeAccount, code: String): Account? {
        return miniglAccountRepository.getAccountByCode(chart, code)
    }

    fun getAccountBalance(account: Account, currency: CurrencyUnit): MonetaryAmount {

        val journal = journalService.getJournal(account.root)
            ?: throw RuntimeException("Unable to find journal for currency: ${account.currencyCode}")

        val glCurrency = miniglCurrencyRepository.getCurrencyByCode(account.currencyCode ?: currency.currencyCode)
            ?: throw RuntimeException("Currency not currently supported on the platform: ${account.currencyCode}")

        return miniglAccountRepository.getBalance(journal, account, glCurrency)
    }

    private fun generateId(parent: Account, padding: Int): String {

        val maxSuffix = miniglAccountRepository.getMaxChildCodeSuffix(parent)
        val nextId = maxSuffix + 1

        return "${parent.code}${nextId.toString().padStart(padding, '0')}"
    }

    @Cacheable(CacheNames.MINIGL_BALANCE_SNAPSHOTS, key = "#chart.id + '_' + #ids.hashCode()")
    fun getAccountBalances(chart: CompositeAccount, ids: Set<String>): List<Pair<String, MonetaryAmount>> {

        val journal = journalService.getJournal(chart)
            ?: throw RuntimeException("Unable to find journal for currency: ${chart.currencyCode}")

        val accounts = miniglAccountRepository.findAccountsByCodesIn(ids)
            .associateBy { it.id }

        val layers = miniglCurrencyRepository.getCurrenciesByCodes(accounts.values.map { it.currencyCode }.toSet())
            .map { it.id.toShort() }

        val balances = latestBalanceSnapshotRepository.getMultipleAccountBalances(
            journal,
            accounts.values.toList(),
            layers = layers.toShortArray()
        )

        return balances.map { (accountId, balance) ->
            val account = accounts[accountId]!!
            val adjustedBalance = if (account.isCredit) balance.negate() else balance
            Pair(account.code, Money.of(adjustedBalance, account.currencyCode))
        }.toList()
    }

    fun findAllByCodes(codes: Set<String>): List<Account> {
        return miniglAccountRepository.findFinalAccountsByCodesIn(codes)
    }

    fun getCompositeAccountByChartAndDescription(chart: CompositeAccount, description: String): CompositeAccount? {
        return miniglAccountRepository.getCompositeAccountByChartAndDescription(chart, description)
    }

    fun getChartByDescription(description: String): CompositeAccount? {
        return miniglAccountRepository.getChartByDescription(description)
    }

    fun getDefaultChart(): CompositeAccount? {
        // Get the first available chart or a specific default chart
        return miniglAccountRepository.getChartByDescription(MiniglConstants.DEFAULT_CHART_NAME)
            ?: miniglAccountRepository.getAllCharts().firstOrNull()
    }

    fun getAccountByCode(code: String): Account? {
        return miniglAccountRepository.getAccountByCode(code)
    }

    fun getAccountsByCurrency(currency: String, pageable: Pageable): List<Account> {
        return miniglAccountRepository.getAccountsByCurrency(currency, pageable)
    }

    fun getAllCurrencies(): List<Currency> {
        return miniglCurrencyRepository.getAllCurrencies()
    }

    fun getAccountsByUserId(userId: UUID): List<Account> {
        // This would need proper implementation based on how user accounts are linked
        return miniglAccountRepository.findAccountsByUserId(userId.toString())
    }

    fun getAccountsByType(type: String, pageable: Pageable): List<Account> {
        return miniglAccountRepository.getAccountsByType(type, pageable)
    }

    @Cacheable(CacheNames.ACCOUNT, key = "'account_' + #id")
    fun getAccountById(id: Long): Account? {
        return miniglAccountRepository.getAccountById(id)
    }

    fun getAccountRunningBalance(
        account: Account,
        currency: CurrencyUnit,
        transactionReference: String
    ): MonetaryAmount {

        val journal = journalService.getJournal(account.root)
            ?: throw RuntimeException("Unable to find journal for currency: ${account.currencyCode}")

        val glCurrency = miniglCurrencyRepository.getCurrencyByCode(account.currencyCode ?: currency.currencyCode)
            ?: throw RuntimeException("Currency not currently supported on the platform: ${account.currencyCode}")

        val maxId = miniglTransactionRepository.getMaxTransactionEntryId(transactionReference)

        return miniglAccountRepository.getRunningBalanceAsOfEntry(journal, account, glCurrency, maxId)
    }
}