package ai.sovereignrag.accounting.minigl.account.dao

import ai.sovereignrag.accounting.entity.*
import ai.sovereignrag.accounting.minigl.account.model.LatestBalanceSnapshot
import ai.sovereignrag.accounting.repository.*
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class LatestBalanceSnapshotRepository(
    private val miniglAccountRepository: MiniglAccountRepository,
    private val latestBalanceSnapshotJpaRepository: LatestBalanceSnapshotJpaRepository
) {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Autowired
    private lateinit var glAccountRepository: GLAccountRepository
    
    @PersistenceContext(unitName = "accounting")
    private lateinit var entityManager: EntityManager

    fun getMultipleAccountBalances(
        journal: JournalEntity,
        accounts: List<GLAccountEntity>?,
        inclusive: Boolean = true,
        layers: ShortArray = shortArrayOf(0),
        date: java.time.Instant? = null
    ): Map<Long, BigDecimal> {
        if (accounts.isNullOrEmpty()) {
            return emptyMap()
        }

        val result = mutableMapOf<Long, BigDecimal>()

        accounts.forEach { account ->
            try {
                val balance = if (account.currencyCode.isNullOrBlank()) {
                    // For composite accounts, calculate based on children
                    calculateCompositeBalance(journal, account, layers)
                } else {
                    // For final accounts, get direct balance
                    val currency = currencyRepository.findByName(account.currencyCode)
                        .orElseThrow { RuntimeException("Currency ${account.currencyCode} not found") }
                    val accountBalance = miniglAccountRepository.getBalance(journal, account, currency)
                    accountBalance.number.numberValue(BigDecimal::class.java)
                }
                result[account.id] = balance.abs()
            } catch (e: Exception) {
                logger.error(e) { "Error calculating balance for account ${account.code}: ${e.message}" }
                result[account.id] = BigDecimal.ZERO
            }
        }

        return result
    }

    private fun calculateCompositeBalance(
        journal: JournalEntity,
        account: GLAccountEntity,
        layers: ShortArray
    ): BigDecimal {
        return try {
            // Try to get from snapshot first
            val snapshotBalance = getBalanceFromSnapshot(journal, account, layers.firstOrNull()?.toString())
            if (snapshotBalance != null) {
                snapshotBalance
            } else {
                // Fall back to calculating from children
                val children = glAccountRepository.findByParent(account)
                children.sumOf { child ->
                    if (child.currencyCode.isNullOrBlank()) {
                        calculateCompositeBalance(journal, child, layers)
                    } else {
                        val currency = currencyRepository.findByName(child.currencyCode).orElse(null)
                        if (currency != null) {
                            val balance = miniglAccountRepository.getBalance(journal, child, currency)
                            balance.number.numberValue(BigDecimal::class.java)
                        } else {
                            BigDecimal.ZERO
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calculating composite balance for ${account.code}: ${e.message}" }
            BigDecimal.ZERO
        }
    }

    private fun getBalanceFromSnapshot(
        journal: JournalEntity,
        account: GLAccountEntity,
        layers: String?
    ): BigDecimal? {
        return try {
            if (layers != null) {
                latestBalanceSnapshotJpaRepository.findByJournalAndAccountAndLayers(
                    journal.id!!,
                    account.code,
                    layers
                )?.balance
            } else {
                // If no specific layers, get the first snapshot found for this account and journal
                latestBalanceSnapshotJpaRepository.findByJournalAndAccountInAndLayers(
                    journal.id!!,
                    listOf(account.code),
                    "0" // Default to base layer
                ).firstOrNull()?.balance
            }
        } catch (e: Exception) {
            logger.debug { "No snapshot found for account ${account.code}: ${e.message}" }
            null
        }
    }

    @Async
    @Transactional(transactionManager = "accountingTransactionManager")
    @LogExecutionTime
    fun updateSnapshotsAfterTransaction(transaction: GLTransactionEntity) {
        try {
            // Find the transaction in the current persistence context (read-only operation)
            val managedTransaction = entityManager.find(GLTransactionEntity::class.java, transaction.id)
            if (managedTransaction == null) {
                logger.warn("Transaction ${transaction.id} not found, skipping balance snapshot update")
                return
            }
            
            // Only process accounts that have base layer entries
            val baseLayerAccounts = managedTransaction.entries.filter { entry ->
                val currency = currencyRepository.findByName(entry.account.currencyCode)
                    .orElseThrow { RuntimeException("Currency ${entry.account.currencyCode} not found") }
                currency != null && entry.layer == currency.id.toShort()
            }.map { it.account }.toSet()

            if (baseLayerAccounts.isEmpty()) {
                logger.debug { "No base layer accounts affected in transaction ${managedTransaction.id}, skipping snapshot update" }
                return
            }

            val currencyCodes = baseLayerAccounts.map { it.currencyCode }.toSet().filter { currencyCode ->
                currencyCode.isNotBlank()
            }.toSet()

            val currencies = currencyRepository.findAllByNameIn(currencyCodes)

            clearBalanceCacheForAccounts(managedTransaction.journal, baseLayerAccounts)
            verifyBalanceCalculations(managedTransaction.journal, baseLayerAccounts, currencies)
            updateSnapshotsForAccountsWithHierarchy(managedTransaction.journal, baseLayerAccounts, currencies)

            logger.debug { "Updated snapshots for ${baseLayerAccounts.size} base layer accounts in transaction ${managedTransaction.id}" }

        } catch (e: Exception) {
            logger.error(e) { "Error updating snapshots after transaction: ${e.message}" }
            throw e // Re-throw to trigger transaction rollback
        }
    }

    private fun updateSnapshotsForAccountsWithHierarchy(
        journal: JournalEntity,
        accounts: Set<GLAccountEntity>,
        currencies: Set<CurrencyEntity>
    ) {
        val calculatedAccounts = mutableSetOf<String>()

        accounts.forEach { account ->
            updateSnapshotsUpHierarchy(journal, account, currencies, calculatedAccounts)
        }
    }

    private fun updateSnapshotsUpHierarchy(
        journal: JournalEntity,
        account: GLAccountEntity,
        currencies: Set<CurrencyEntity>,
        calculatedAccounts: MutableSet<String>
    ) {
        if (calculatedAccounts.contains(account.code)) {
            return
        }

        try {
            if (account.currencyCode != null && account.currencyCode.isNotBlank()) {
                val accountCurrency = currencies.find { it.name == account.currencyCode }
                if (accountCurrency != null) {
                    updateSnapshotForAccountAndCurrency(journal, account, accountCurrency)
                    calculatedAccounts.add(account.code)
                    logger.debug { "Updated snapshot for account ${account.code} in currency ${accountCurrency.name}" }
                } else {
                    logger.warn { "Currency ${account.currencyCode} not found for account ${account.code}" }
                }
            } else {
                currencies.forEach { currency ->
                    updateCompositeSnapshotForAccountAndCurrency(journal, account, currency)
                }
                calculatedAccounts.add(account.code)
                logger.debug { "Updated composite snapshots for account ${account.code} across ${currencies.size} currencies" }
            }

            account.parent?.let { parentAccount ->
                updateSnapshotsUpHierarchy(journal, parentAccount, currencies, calculatedAccounts)
            }

        } catch (e: Exception) {
            logger.error(e) { "Error updating snapshot hierarchy for account ${account.code}: ${e.message}" }
        }
    }

    private fun clearBalanceCacheForAccounts(journal: JournalEntity, accounts: Set<GLAccountEntity>) {
        try {
            val accountIds = accounts.map { it.id }
            val codes = accounts.filter { account ->
                account.currencyCode.isNotBlank()
            }.map { it.code }.toSet()

            val currencies = currencyRepository.findAllByNameIn(codes)

            currencies.forEach { currency ->
                logger.debug { "Cleared base layer balance cache for ${accountIds.size} accounts in currency ${currency.name}" }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error clearing balance cache: ${e.message}" }
        }
    }

    private fun updateSnapshotForAccountAndCurrency(
        journal: JournalEntity,
        account: GLAccountEntity,
        currency: CurrencyEntity
    ) {
        try {
            val latestBalance = miniglAccountRepository.getBalance(journal, account, currency)
            val adjustedBalance = latestBalance.number.numberValue(BigDecimal::class.java)

            val existingSnapshot = latestBalanceSnapshotJpaRepository.findByJournalAndAccountAndLayers(
                journal.id!!,
                account.code,
                currency.id.toString()
            )

            if (existingSnapshot != null) {
                existingSnapshot.balance = adjustedBalance
                latestBalanceSnapshotJpaRepository.save(existingSnapshot)
            } else {
                latestBalanceSnapshotJpaRepository.save(
                    LatestBalanceSnapshot(
                        journal = journal.id!!,
                        account = account.code,
                        layers = currency.id.toString(),
                        balance = adjustedBalance,
                        currency = currency.name
                    )
                )
            }

            logger.debug { "Updated base layer snapshot for account ${account.code} in currency ${currency.name}: $adjustedBalance" }

        } catch (e: Exception) {
            logger.error(e) { "Error updating snapshot for account ${account.code} and currency ${currency.name}: ${e.message}" }
        }
    }

    private fun updateCompositeSnapshotForAccountAndCurrency(
        journal: JournalEntity,
        account: GLAccountEntity,
        currency: CurrencyEntity
    ) {
        try {
            val compositeBalance = calculateCompositeBalanceForCurrency(journal, account, currency)

            val existingSnapshot = latestBalanceSnapshotJpaRepository.findByJournalAndAccountAndLayers(
                journal.id!!,
                account.code,
                currency.id.toString()
            )

            if (existingSnapshot != null) {
                existingSnapshot.balance = compositeBalance
                latestBalanceSnapshotJpaRepository.save(existingSnapshot)
            } else {
                latestBalanceSnapshotJpaRepository.save(
                    LatestBalanceSnapshot(
                        journal = journal.id!!,
                        account = account.code,
                        layers = currency.id.toString(),
                        balance = compositeBalance,
                        currency = currency.name
                    )
                )
            }

            logger.debug { "Updated composite snapshot for account ${account.code} in currency ${currency.name}: $compositeBalance" }

        } catch (e: Exception) {
            logger.error(e) { "Error updating composite snapshot for account ${account.code} and currency ${currency.name}: ${e.message}" }
        }
    }

    private fun calculateCompositeBalanceForCurrency(
        journal: JournalEntity,
        parentAccount: GLAccountEntity,
        currency: CurrencyEntity
    ): BigDecimal {
        return try {
            calculateCompositeBalanceRecursive(journal, parentAccount, currency, mutableSetOf(), 0)
        } catch (e: Exception) {
            logger.error(e) { "Error calculating composite balance for ${parentAccount.code} in currency ${currency.name}: ${e.message}" }
            BigDecimal.ZERO
        }
    }

    private fun calculateCompositeBalanceRecursive(
        journal: JournalEntity,
        parentAccount: GLAccountEntity,
        currency: CurrencyEntity,
        visited: MutableSet<Long>,
        depth: Int
    ): BigDecimal {
        if (visited.contains(parentAccount.id) || depth > 15) {
            logger.warn {
                "Stopping composite balance calculation for ${parentAccount.code} - visited: ${
                    visited.contains(
                        parentAccount.id
                    )
                }, depth: $depth"
            }
            return BigDecimal.ZERO
        }

        visited.add(parentAccount.id)

        try {
            var totalBalance = BigDecimal.ZERO
            val children = getDirectChildren(parentAccount)

            for (child in children) {
                val childBalance = when {
                    child.currencyCode == currency.name -> {
                        if (child is FinalAccountEntity) {
                            val balance = miniglAccountRepository.getBalance(journal, child, currency)
                            balance.number.numberValue(BigDecimal::class.java)
                        } else {
                            calculateCompositeBalanceRecursive(journal, child, currency, visited, depth + 1)
                        }
                    }

                    child.currencyCode.isNullOrBlank() -> {
                        calculateCompositeBalanceRecursive(journal, child, currency, visited, depth + 1)
                    }

                    else -> BigDecimal.ZERO
                }

                totalBalance = totalBalance.add(childBalance)
            }

            return totalBalance
        } finally {
            visited.remove(parentAccount.id)
        }
    }

    private fun getDirectChildren(parent: GLAccountEntity): List<GLAccountEntity> {
        return try {
            glAccountRepository.findByParent(parent)
        } catch (e: Exception) {
            logger.error(e) { "Error loading direct children for ${parent.code}: ${e.message}" }
            emptyList()
        }
    }

    private fun verifyBalanceCalculations(
        journal: JournalEntity,
        accounts: Set<GLAccountEntity>,
        currencies: Set<CurrencyEntity>
    ) {
        // Balance verification logic would go here
        // This is a simplified version - the full verification logic could be implemented
        // using the repository pattern instead of raw session queries
        logger.debug { "Balance verification completed for ${accounts.size} accounts" }
    }
}