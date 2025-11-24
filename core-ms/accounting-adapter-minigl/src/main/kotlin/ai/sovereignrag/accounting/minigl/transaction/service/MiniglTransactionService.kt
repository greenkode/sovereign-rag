package ai.sovereignrag.accounting.minigl.transaction.service

import ai.sovereignrag.accounting.Tags
import ai.sovereignrag.accounting.gateway.api.request.CreateTransactionRequest
import ai.sovereignrag.accounting.gateway.api.request.TransactionEntryRequest
import ai.sovereignrag.accounting.minigl.account.dao.LatestBalanceSnapshotRepository
import ai.sovereignrag.accounting.minigl.account.dao.MiniglAccountRepository
import ai.sovereignrag.accounting.minigl.currency.dao.MiniglCurrencyRepository
import ai.sovereignrag.accounting.minigl.journal.JournalService
import ai.sovereignrag.accounting.minigl.transaction.dao.MiniglTransactionRepository
import ai.sovereignrag.accounting.minigl.transaction.dto.TransactionDetail
import ai.sovereignrag.accounting.minigl.transaction.service.bridge.BridgeAccountResolver
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryBuilderPayload
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntrySpecExecutor
import ai.sovereignrag.accounting.minigl.transaction.service.builder.LayerType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionContext
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionEntryStrategyFactory
import ai.sovereignrag.accounting.minigl.transaction.service.validation.CompositeTransactionValidator
import ai.sovereignrag.accounting.minigl.transaction.service.validation.ValidationContext
import ai.sovereignrag.commons.cache.CacheNames
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.performance.LogExecutionTime
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

// Layer offset constants for backward compatibility
const val PENDING_OFFSET: Short = 1000
const val CREDIT_ALLOWANCES_OFFSET: Short = 2000
const val ON_HOLD_OFFSET: Short = 3000
const val DAILY_LIMIT_OFFSET: Short = 4000
const val CUMULATIVE_LIMIT_OFFSET: Short = 5000

@Service
class MiniglTransactionService(
    private val miniglTransactionRepository: MiniglTransactionRepository,
    private val journalService: JournalService,
    private val miniglAccountRepository: MiniglAccountRepository,
    private val miniglCurrencyRepository: MiniglCurrencyRepository,
    private val bridgeAccountResolver: BridgeAccountResolver,
    private val validator: CompositeTransactionValidator,
    private val strategyFactory: TransactionEntryStrategyFactory,
    private val entryExecutor: EntrySpecExecutor,
    private val accountBalanceSnapshotRepository: LatestBalanceSnapshotRepository
) {

    private val movementPrinter = TransactionMovementPrinter()

    @Transactional(transactionManager = "accountingTransactionManager")
    @LogExecutionTime
    @CacheEvict(value = [CacheNames.MINIGL_BALANCE_SNAPSHOTS, CacheNames.ACCOUNT_BALANCE, CacheNames.MINIGL_TRANSACTIONS], allEntries = true)
    fun createTransaction(request: CreateTransactionRequest, chart: CompositeAccount): TransactionDetail {

        // Create transaction context
        val context = createTransactionContext(request, chart)

        // Validate the request
        validator.validate(request, ValidationContext(context))

        // Create GL transaction
        val glTransaction = createGLTransaction(request, chart)

        // Process each entry using strategies
        request.entries.forEach { entryRequest ->
            val payload = createEntryBuilderPayload(glTransaction, entryRequest, context)
            val strategy = strategyFactory.getStrategy(entryRequest, context)
            val specs = strategy.createEntries(payload)
            entryExecutor.executeSpecs(glTransaction, specs)
        }
        glTransaction.tags.add("group:${request.group}")
        glTransaction.tags.add("type:${request.type}")


        // Print completion movements
        movementPrinter.printTransactionMovements(glTransaction, "Pre Created")

        // Post transaction
        miniglTransactionRepository.postTransaction(glTransaction)

        accountBalanceSnapshotRepository.updateSnapshotsAfterTransaction(glTransaction)

        // Print transaction movements
        movementPrinter.printTransactionMovements(glTransaction, "Created")

        return TransactionDetail(
            request.reference.toString(),
            request.metadata + mapOf(
                "account_ids" to glTransaction.entries.map { it.account.code }.toSet().joinToString(","),
                "transactions" to glTransaction.detail
            )
        )
    }

    private fun createTransactionContext(
        request: CreateTransactionRequest,
        chart: CompositeAccount
    ): TransactionContext {

        val accountCodes = request.entries.flatMap { listOf(it.creditAccount, it.debitAccount) }.toSet()
        val accounts = miniglAccountRepository.findFinalAccountsByCodesIn(accountCodes).associateBy { it.code }
        val currencies = miniglCurrencyRepository.getCurrenciesByCodes(accounts.values.map { it.currencyCode }.toSet())
            .associateBy { it.name }
        val bridgeAccounts = bridgeAccountResolver.getAllBridgeAccounts(accounts.values, chart)

        return TransactionContext(
            isPending = request.pending,
            chart = chart,
            currencies = currencies,
            accounts = accounts,
            bridgeAccounts = bridgeAccounts,
            type = request.type.toString(),
            group = request.group.toString(),
        )
    }

    private fun createGLTransaction(request: CreateTransactionRequest, chart: CompositeAccount): GLTransaction {
        val journal = journalService.getJournal(chart)
            ?: throw IllegalArgumentException("Journal not found for chart: ${chart.description}")

        return GLTransaction().apply {
            timestamp = Instant.now()
            postDate = Instant.now()
            detail = request.reference.toString()
            this.journal = journal
            tags = Tags(request.metadata.entries.joinToString(",") { "${it.key}:${it.value}" })
        }
    }

    private fun createEntryBuilderPayload(
        transaction: GLTransaction,
        entryRequest: TransactionEntryRequest,
        context: TransactionContext
    ): EntryBuilderPayload {
        val creditAccount = context.accounts[entryRequest.creditAccount]
            ?: throw RuntimeException("Credit account not found: ${entryRequest.creditAccount}")
        val debitAccount = context.accounts[entryRequest.debitAccount]
            ?: throw RuntimeException("Debit account not found: ${entryRequest.debitAccount}")
        val currency = context.currencies[entryRequest.amount.currency.currencyCode]
            ?: throw RuntimeException("Currency not found: ${entryRequest.amount.currency}")

        val bridgeAssetAccount = bridgeAccountResolver.resolveAssetBridge(debitAccount, context.chart)
        val bridgeLiabilityAccount = bridgeAccountResolver.resolveLiabilityBridge(debitAccount, context.chart)

        return EntryBuilderPayload(
            transaction = transaction,
            entry = entryRequest,
            layer = currency,
            debitAccount = debitAccount,
            creditAccount = creditAccount,
            bridgeAssetAccount = bridgeAssetAccount,
            bridgeLiabilityAccount = bridgeLiabilityAccount
        )
    }

    @Transactional(transactionManager = "accountingTransactionManager")
    fun reverseTransaction(reference: String, reversalReference: String): TransactionDetail {

        miniglTransactionRepository.findTransactionGroup(reference)?.let { glTransactionGroup ->


            // Check if all transactions are already reversed for idempotency
            val alreadyReversed = glTransactionGroup.transactions.filter {
                (it as GLTransaction).tags.contains("reversed:true")
            }

            if (alreadyReversed.isNotEmpty()) {
                // If all transactions are already reversed, return success (idempotent)
                if (alreadyReversed.size == glTransactionGroup.transactions.size) {
                    return TransactionDetail(
                        reference,
                        mapOf(
                            "account_ids" to glTransactionGroup.transactions.map { transaction ->
                                transaction as GLTransaction
                                transaction.entries.map { it.account.code }.toSet().joinToString(",")
                            }.joinToString(",") { it },
                            "transactions" to glTransactionGroup.transactions.joinToString(",") { (it as GLTransaction).detail },
                            "status" to "already_reversed"
                        )
                    )
                }
                // Partial reversal is an error
                throw RuntimeException("One or more transactions in group: $reference have already been reversed")
            }

            val result = glTransactionGroup.transactions.sortedByDescending {
                it as GLTransaction
                it.postDate
            }.map { transaction ->
                transaction as GLTransaction
                miniglTransactionRepository.reverseTransaction(transaction, UUID.randomUUID().toString())
            }.toSet()

            glTransactionGroup.transactions.forEach { transaction ->
                transaction as GLTransaction
                accountBalanceSnapshotRepository.updateSnapshotsAfterTransaction(transaction)
                // Print reversal movements
                movementPrinter.printTransactionMovements(transaction, "Reversed (Group)")
            }

            return TransactionDetail(
                reference,
                mapOf(
                    "account_ids" to glTransactionGroup.transactions.map { transaction ->
                        transaction as GLTransaction
                        transaction.entries.map { it.account.code }.toSet().joinToString(",")
                    }.joinToString(",") { it },
                    "transactions" to result.joinToString(",") { it }
                )
            )
        }

        val transaction = miniglTransactionRepository.findByReference(reference)
            ?: throw RecordNotFoundException("Unable to find transaction with reference: $reference")

        if (transaction.tags.contains("reversed:true")) {
            // Transaction is already reversed, return success (idempotent)
            return TransactionDetail(
                reference,
                mapOf(
                    "account_ids" to transaction.entries.map { it.account.code }.toSet().joinToString(","),
                    "transactions" to reference,
                    "status" to "already_reversed"
                )
            )
        }

        miniglTransactionRepository.reverseTransaction(transaction, reversalReference)

        accountBalanceSnapshotRepository.updateSnapshotsAfterTransaction(transaction)

        // Print reversal movements
        movementPrinter.printTransactionMovements(transaction, "Reversed")

        return TransactionDetail(
            reversalReference,
            mapOf(
                "account_ids" to transaction.entries.map { it.account.code }.toSet().joinToString(","),
                "transactions" to listOf(reference, reversalReference).joinToString(",") { it }
            )
        )
    }

    @Transactional(transactionManager = "accountingTransactionManager")
    fun completeTransaction(reference: String): TransactionDetail {
        val transaction = miniglTransactionRepository.findByReference(reference)
            ?: throw RuntimeException("Unable to find transaction with reference: $reference")

        // Check for idempotency early
        if (transaction.tags.contains("completed:true")) {
            // Transaction is already completed, return success (idempotent)
            return TransactionDetail(
                reference,
                mapOf(
                    "account_ids" to transaction.entries.map { it.account.code }.toSet().joinToString(","),
                    "transactions" to reference,
                    "status" to "already_completed"
                )
            )
        }

        val currencies = miniglCurrencyRepository.getCurrenciesByCodes(
            transaction.entries.map { it.account.currencyCode }.toSet()
        )
        val currenciesLayer = currencies.associateBy({ it.name }, { it.id.toShort() })

        val completion = createCompletionTransaction(transaction)

        val transactionGroup = transaction.tags?.toString()?.split(",")
            ?.find { it.startsWith("group:") }
            ?.substringAfter("group:")
            ?: throw RuntimeException("Unable to find transaction group for reference: $reference. Please try again later or contact support if this issue persists.")

        val transactionType = transaction.tags?.toString()?.split(",")
            ?.find { it.startsWith("type:") }
            ?.substringAfter("type:")
            ?: throw RuntimeException("Unable to find transaction type for reference: $reference. Please try again later or contact support if this issue persists.")

        val isPending = transaction.entries.any { entry ->
            currenciesLayer.values.any { baseLayer ->
                entry.layer == (LayerType.PENDING.offset.toInt() + baseLayer).toShort()
            }
        }

        val context = TransactionContext(
            isPending,
            transaction.entries.first().account.root,
            mapOf(),
            mapOf(),
            mapOf(),
            transactionGroup,
            transactionType,
        )

        // Get the appropriate strategy
        val strategy = strategyFactory.getStrategy(context)

        // Use strategy-specific completion logic
        strategy.completeTransaction(transaction, completion, currenciesLayer)

        // Print completion movements
        movementPrinter.printTransactionMovements(completion, "Pre Completed")

        miniglTransactionRepository.completeTransaction(transaction, completion)

        accountBalanceSnapshotRepository.updateSnapshotsAfterTransaction(transaction)

        // Print completion movements
        movementPrinter.printTransactionMovements(completion, "Completed")

        return TransactionDetail(
            reference,
            mapOf(
                "account_ids" to transaction.entries.map { it.account.code }.toSet().joinToString(","),
                "transactions" to listOf(reference, completion.detail).joinToString(",")
            )
        )
    }

    private fun createCompletionTransaction(originalTransaction: GLTransaction): GLTransaction {
        return GLTransaction().apply {
            timestamp = Instant.now()
            postDate = Instant.now()
            detail = UUID.randomUUID().toString()
            journal = originalTransaction.journal
            tags = Tags(originalTransaction.tags.toString())
            tags.add("completes:${originalTransaction.detail}")
        }
    }
}