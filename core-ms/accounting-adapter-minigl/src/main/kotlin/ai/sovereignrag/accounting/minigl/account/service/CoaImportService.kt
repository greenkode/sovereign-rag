package ai.sovereignrag.accounting.minigl.account.service


import ai.sovereignrag.accounting.GLException
import ai.sovereignrag.accounting.minigl.currency.dao.MiniglCurrencyRepository
import ai.sovereignrag.accounting.repository.CurrencyRepository
import ai.sovereignrag.accounting.repository.GLAccountRepository
import ai.sovereignrag.accounting.repository.JournalRepository
import ai.sovereignrag.accounting.repository.LayerRepository
import ai.sovereignrag.accounting.repository.RuleInfoRepository
import ai.sovereignrag.commons.coa.ChartOfAccounts
import ai.sovereignrag.commons.coa.ChartOfAccountsRequest
import ai.sovereignrag.commons.coa.CompositeAccountRequest
import ai.sovereignrag.commons.coa.CurrencyRequest
import ai.sovereignrag.commons.coa.FinalAccountRequest
import ai.sovereignrag.commons.coa.JournalRequest
import ai.sovereignrag.commons.coa.LayerRequest
import ai.sovereignrag.commons.coa.RuleRequest
import ai.sovereignrag.commons.enumeration.ResponseCode
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.CurrencyEntity as Currency
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLAccountEntity as Account
import ai.sovereignrag.accounting.entity.JournalEntity as Journal
import ai.sovereignrag.accounting.entity.LayerEntity as Layer
import ai.sovereignrag.accounting.entity.RuleInfoEntity as RuleInfo

@Service
class CoaImportService(
    private val currencyRepository: MiniglCurrencyRepository,
    private val jpaAccountRepository: GLAccountRepository,
    private val jpaCurrencyRepository: CurrencyRepository,
    private val journalRepository: JournalRepository,
    private val layerRepository: LayerRepository,
    private val ruleInfoRepository: RuleInfoRepository
) {

    var strictAccountCodes: Boolean = true

    private val log = KotlinLogging.logger {}


    @LogExecutionTime
    @Transactional(transactionManager = "accountingTransactionManager")
    fun import(chart: ChartOfAccountsRequest): CompositeAccount? {
        createCurrencies(chart.currencies)
        createChart(chart.chartOfAccounts)
        createJournals(chart.journals, chart.chartOfAccounts.description)

        return getChartByCodeAndDescription(chart.chartOfAccounts.code, chart.chartOfAccounts.description)
    }

    private fun createChart(chart: ChartOfAccounts) {
        val acct =
            getChartByCodeAndDescription(chart.code, chart.description) ?: CompositeAccount().apply {
                code = chart.code
                currencyCode = chart.currency
                description = chart.description
                created = chart.created
            }
        val savedAcct = jpaAccountRepository.save(acct)
        savedAcct.root = savedAcct
        processChartChildren(savedAcct, chart.compositeAccounts, emptyList())
    }

    private fun createCurrencies(currencies: List<CurrencyRequest>) {
        currencies.forEach { currency ->
            if (!currencyExists(currency.name)) {
                val currencyEntity = Currency().apply {
                    id = currency.id
                    name = currency.name
                    symbol = currency.symbol
                }
                currencyRepository.getCurrencyByCode(currencyEntity.name) ?: jpaCurrencyRepository.save(currencyEntity)
            }
        }
    }

    private fun currencyExists(name: String): Boolean {
        return currencyRepository.getCurrencyByCode(name) != null
    }

    private fun createJournals(journals: List<JournalRequest>, description: String?) {
        journals.forEach { j ->
            val chart = getChartByCodeAndDescription(j.chart, description ?: "")

            val journal = getJournal(chart, j.name) ?: Journal()
            journal.name = j.name
            journal.start = j.start

            journal.chart = chart
            val savedJournal = journalRepository.save(journal)

            createJournalRules(savedJournal, j.rules)
            createJournalLayers(savedJournal, j.layers)
        }
    }

    private fun processChartChildren(
        parent: CompositeAccount, compositeChildren: List<CompositeAccountRequest>,
        finalChildren: List<FinalAccountRequest>
    ) {
        compositeChildren.forEach { child ->
            createComposite(parent, child)
        }

        finalChildren.forEach { child ->
            createFinal(parent, child)
        }
    }


    private fun validateAccountCode(parent: Account, child: Account) {
        if (!parent.isChart && !child.code.startsWith(parent.code)) {
            throw GLException("Child account code `" + child.code + "` must start with parent account code `" + parent.code + "`", ResponseCode.INVALID_ACCOUNT)
        }
    }


    private fun createComposite(parentAcc: CompositeAccount, elem: CompositeAccountRequest) {
        val acct = getAccount(parentAcc.root, elem.code) ?: CompositeAccount().apply {
            code = elem.code
            type = parentAcc.type
            description = elem.description
            currencyCode = elem.currency
            root = parentAcc.root
            parent = parentAcc
            created = Instant.now()
        }
        if (strictAccountCodes) validateAccountCode(parentAcc, acct)

        acct.code = elem.code
        acct.type = when (elem.type) {
            "debit" -> 1
            "credit" -> 2
            else -> 0
        }
        acct.description = elem.description
        acct.created = Instant.now()
        acct.root = parentAcc.root
        val savedAcct = jpaAccountRepository.save(acct)
        savedAcct.parent = parentAcc
        parentAcc.getChildren().add(savedAcct)
        jpaAccountRepository.save(parentAcc)
        processChartChildren(savedAcct, elem.compositeAccounts, elem.accounts)
    }

    private fun createFinal(parent: CompositeAccount, elem: FinalAccountRequest) {
        val acct = getFinalAccount(parent.root, elem.code) ?: FinalAccount()

        acct.code = elem.code
        acct.type = elem.type.toInt()
        acct.description = elem.description

        if (strictAccountCodes) validateAccountCode(parent, acct)

        acct.root = parent.root
        val savedAcct = jpaAccountRepository.save(acct)
        savedAcct.parent = parent
        parent.getChildren().add(savedAcct)
    }

    private fun createJournalRules(journal: Journal, ruleRequests: List<RuleRequest>) {
        ruleRequests.forEach { rule ->
            val ri = RuleInfo()
            ri.description = rule.description
            ri.clazz = rule.clazz
            ri.journal = journal
            ri.param = rule.param
            ri.layers = rule.layers
            ri.account = getAccount(journal.chart, journal.chart.code)
            ruleInfoRepository.save(ri)
        }
    }

    private fun createJournalLayers(journal: Journal?, layers: List<LayerRequest>) {
        layers.forEach { l ->
            val journalLayers = journal?.layers ?: listOf<Layer>()

            journalLayers.map {
                it as Layer
                it.id
            }.contains(l.id).let {
                if (!it) {
                    val layer = Layer()
                    layer.id = l.id
                    layer.name = l.name
                    layer.journal = journal
                    layerRepository.save(layer)
                }
            }
        }
    }


    private fun getAccount(chart: Account, accountCode: String): CompositeAccount? {
        return jpaAccountRepository.findCompositeAccountByCodeAndRoot(accountCode, chart).orElse(null)
    }

    private fun getFinalAccount(chart: Account, code: String): FinalAccount? {
        return jpaAccountRepository.findFinalAccountByCodeAndRoot(code, chart).orElse(null)
    }

    private fun getChartByCodeAndDescription(chartCode: String?, descrition: String): CompositeAccount? {
        return jpaAccountRepository.findChartByCodeAndDescription(chartCode, descrition).orElse(null)

    }

    private fun getJournal(chart: CompositeAccount?, name: String): Journal? {
        return chart?.let {
            journalRepository.findByNameAndChart(name, it).orElse(null)
        }
    }
}

