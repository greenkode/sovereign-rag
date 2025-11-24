package ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies

import ai.sovereignrag.accounting.minigl.transaction.service.builder.BaseEntryStrategy
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryBuilderPayload
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntrySpec
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.LayerType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionContext
import java.math.BigDecimal
import ai.sovereignrag.accounting.entity.GLAccountEntity as Account
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

class PendingInboundTransactionStrategy : BaseEntryStrategy() {

    override fun canHandle(context: TransactionContext): Boolean {
        return context.isPending && listOf("INBOUND").contains(context.group)
    }

    override fun createBaseLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        val amount = payload.entry.amount.number.numberValue(BigDecimal::class.java)
        val detail = payload.entry.detail

        val bridgeLiabilityAccount = payload.bridgeLiabilityAccount
            ?: throw RuntimeException("Bridge liability account not found")

        val layer = getBaseLayer(payload.layer)
        return listOf(
            EntrySpec(payload.debitAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(
                bridgeLiabilityAccount,
                amount,
                EntryType.CREDIT,
                layer,
                detail,
                "credit: ${payload.entry.creditAccount}"
            )
        )
    }

    override fun createOffsetLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        // All entries (including fees, commissions, rebates) need offset layer entries
        val amount = payload.entry.amount.number.numberValue(BigDecimal::class.java)
        val pendingLayer = getPendingLayer(payload.layer)
        val detail = payload.entry.detail

        val bridgeAssetAccount = payload.bridgeAssetAccount
            ?: throw RuntimeException("Bridge asset account not found")
        val bridgeLiabilityAccount = payload.bridgeLiabilityAccount
            ?: throw RuntimeException("Bridge liability account not found")

        return when (payload.creditAccount.type) {
            Account.CREDIT -> createCreditAccountPendingEntries(
                amount, pendingLayer, detail, bridgeAssetAccount, bridgeLiabilityAccount, payload.creditAccount
            )

            Account.DEBIT -> createDebitAccountPendingEntries(
                amount, pendingLayer, detail, bridgeLiabilityAccount, payload.creditAccount
            )

            else -> throw RuntimeException("Unknown account type: ${payload.creditAccount.type}")
        }
    }

    private fun createCreditAccountPendingEntries(
        amount: BigDecimal,
        layer: Short,
        detail: String,
        bridgeAssetAccount: FinalAccount,
        bridgeLiabilityAccount: FinalAccount,
        creditAccount: FinalAccount
    ): List<EntrySpec> {
        return listOf(
            // Bridge the asset and liability
            EntrySpec(bridgeAssetAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(bridgeLiabilityAccount, amount, EntryType.CREDIT, layer, detail),

            // Complete the flow to final credit account
            EntrySpec(bridgeLiabilityAccount, amount, EntryType.DEBIT, layer, detail, ),
            EntrySpec(creditAccount, amount, EntryType.CREDIT, layer, detail)
        )
    }

    private fun createDebitAccountPendingEntries(
        amount: BigDecimal,
        layer: Short,
        detail: String,
        bridgeLiabilityAccount: FinalAccount,
        creditAccount: FinalAccount
    ): List<EntrySpec> {
        return listOf(
            // For debit-type credit accounts (like cash), we debit them to increase balance
            EntrySpec(creditAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(bridgeLiabilityAccount, amount, EntryType.CREDIT, layer, detail, ),
        )
    }

    override fun createLimitEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        // Don't create limit entries for pending transactions as they are already on pending layer
        return emptyList()
    }
    
    // Default completion logic - moved from TransactionService
    override fun completeTransaction(
        originalTransaction: GLTransaction,
        completionTransaction: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        // 1. Reverse all offset layer entries (pending, hold, etc.)
        reverseOffsetLayers(originalTransaction, completionTransaction, currenciesLayer)
        
        // 2. Create base layer entries for accounts tagged with "credit" on offset layers
        completeTaggedCredits(originalTransaction, completionTransaction, currenciesLayer)
    }
    
    private fun reverseOffsetLayers(
        transaction: GLTransaction,
        completion: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        // Get all offset layers (non-base layers)
        val offsetLayers = currenciesLayer.values.flatMap { baseLayer ->
            listOf(
                (LayerType.PENDING.offset.toInt() + baseLayer).toShort(),
                (LayerType.ON_HOLD.offset.toInt() + baseLayer).toShort(),
                (LayerType.FEE.offset.toInt() + baseLayer).toShort(),
                (LayerType.DAILY_LIMIT.offset.toInt() + baseLayer).toShort(),
                (LayerType.CUMULATIVE_LIMIT.offset.toInt() + baseLayer).toShort(),
                (LayerType.CREDIT_ALLOWANCES.offset.toInt() + baseLayer).toShort()
            )
        }
        
        // Reverse all entries on offset layers
        val reversalTxn = transaction.createReverse(false, *offsetLayers.toShortArray())
        
        reversalTxn.entries.forEach { reversalEntry ->
            val glEntry = reversalEntry as GLEntry
            val newEntry = if (glEntry.isCredit) {
                completion.createCredit(glEntry.account, glEntry.amount, glEntry.detail, glEntry.layer)
            } else {
                completion.createDebit(glEntry.account, glEntry.amount, glEntry.detail, glEntry.layer)
            }
            newEntry.tags = completion.tags
        }
    }
    
    private fun completeTaggedCredits(
        transaction: GLTransaction,
        completion: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        val creditTaggedEntries = transaction.entries
            .filter { entry ->
                entry.tags?.toString()
                    ?.split(",")
                    ?.any { it.contains("credit") } == true
            }
        
        creditTaggedEntries.forEach { offsetEntry ->
            val currencyLayer = currenciesLayer[offsetEntry.account.currencyCode] ?: 0
            
            val recipient = transaction.entries.first { entry ->
                entry.account.code == offsetEntry.tags.toString().split(",").first { it.startsWith("credit:") }
                    .split(":")[1].trim()
            }
            
            val baseLayerEntry =
                completion.createCredit(
                    recipient.account,
                    offsetEntry.amount,
                    "Completion credit for ${offsetEntry.detail}",
                    currencyLayer // base layer
                )
            
            baseLayerEntry.tags = completion.tags
            
            val bridgeEntry =
                completion.createDebit(
                    offsetEntry.account,
                    offsetEntry.amount,
                    "Bridge debit for completion",
                    currencyLayer
                )
            
            bridgeEntry.tags = completion.tags
        }
        
        val debitTaggedEntries = transaction.entries
            .filter { entry ->
                entry.tags?.toString()
                    ?.split(",")
                    ?.any { it.contains("debit") } == true
            }
        
        debitTaggedEntries.forEach { offsetEntry ->
            val currencyLayer = currenciesLayer[offsetEntry.account.currencyCode] ?: 0
            
            val recipient = transaction.entries.first { entry ->
                entry.account.code == offsetEntry.tags.toString().split(",").first { it.startsWith("debit:") }
                    .split(":")[1].trim()
            }
            
            val baseLayerEntry =
                completion.createDebit(
                    recipient.account,
                    offsetEntry.amount,
                    "Completion credit for ${offsetEntry.detail}",
                    currencyLayer // base layer
                )
            
            baseLayerEntry.tags = completion.tags
            
            val bridgeAccount = findBridgeAccount(transaction, offsetEntry.account)
            bridgeAccount?.let { bridge ->
                val bridgeEntry =
                    completion.createCredit(
                        bridge as FinalAccount,
                        offsetEntry.amount,
                        "Bridge debit for completion",
                        currencyLayer
                    )
                
                bridgeEntry.tags = completion.tags
            }
        }
    }
    
    private fun findBridgeAccount(transaction: GLTransaction, account: Account): Account? {
        return transaction.entries.find { entry ->
            entry.account.description.startsWith("bridge-liabilities") &&
                    entry.account.currencyCode == account.currencyCode
        }?.account
    }
}