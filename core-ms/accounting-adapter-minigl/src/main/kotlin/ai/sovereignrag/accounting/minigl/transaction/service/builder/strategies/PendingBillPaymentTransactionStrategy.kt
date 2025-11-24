package ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies

import ai.sovereignrag.accounting.Tags
import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import ai.sovereignrag.accounting.minigl.transaction.service.builder.BaseEntryStrategy
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryBuilderPayload
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntrySpec
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.LayerType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionContext
import ai.sovereignrag.accounting.entity.GLAccountEntity as Account
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction
import java.math.BigDecimal

class PendingBillPaymentTransactionStrategy : BaseEntryStrategy() {

    override fun canHandle(context: TransactionContext): Boolean {
        return context.isPending && context.group == MiniglConstants.BILL_PAYMENT_GROUP.name
    }

    override fun createBaseLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {

        if (payload.entry.metadata["type"] == MiniglConstants.EntryTypes.AMOUNT) {

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
                    "credit:${payload.entry.creditAccount},type:AMOUNT"
                )
            )
        }

        return listOf()
    }

    private fun getTag(tags: Tags, name: String): String? {
        return tags.toString().split(",").first { it.startsWith(name) }
            .split(":")[1]
    }

    override fun createOffsetLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {

        val amount = payload.entry.amount.number.numberValue(BigDecimal::class.java)
        val pendingLayer = getPendingLayer(payload.layer)
        val detail = payload.entry.detail

        val type = payload.entry.metadata["type"]

        val bridgeAssetAccount = payload.bridgeAssetAccount
            ?: throw RuntimeException("Bridge asset account not found")
        val bridgeLiabilityAccount = payload.bridgeLiabilityAccount
            ?: throw RuntimeException("Bridge liability account not found")

        if (type == MiniglConstants.EntryTypes.REBATE || type == MiniglConstants.EntryTypes.AMOUNT) {

            return when (payload.creditAccount.type) {
                Account.CREDIT -> createCreditAccountPendingEntries(
                    amount,
                    pendingLayer,
                    detail,
                    bridgeAssetAccount,
                    bridgeLiabilityAccount,
                    payload.creditAccount, "debit:${payload.entry.debitAccount},type:REBATE"
                )

                Account.DEBIT -> createDebitAccountPendingEntries(
                    amount, pendingLayer, detail, bridgeLiabilityAccount, payload.creditAccount
                )

                else -> throw RuntimeException("Unknown account type: ${payload.debitAccount.type}")
            }
        } else if (type == MiniglConstants.EntryTypes.COMMISSION) {

            if (getTag(payload.debitAccount.tags, "type") != "EXPENSE")
                throw RuntimeException("Expense Account not found")

            return when (payload.creditAccount.type) {

                Account.CREDIT -> createCreditAccountPendingEntries(
                    amount, pendingLayer, detail, payload.debitAccount, bridgeLiabilityAccount, payload.creditAccount, "debit:${payload.debitAccount.code},type:COMMISSION"
                )

                Account.DEBIT -> createDebitAccountPendingEntries(
                    amount, pendingLayer, detail, payload.debitAccount, payload.creditAccount
                )

                else -> throw RuntimeException("Unknown account type: ${payload.debitAccount.type}")
            }
        }

        return emptyList()
    }

    private fun createCreditAccountPendingEntries(
        amount: BigDecimal,
        layer: Short,
        detail: String,
        bridgeAssetAccount: FinalAccount,
        bridgeLiabilityAccount: FinalAccount,
        creditAccount: FinalAccount,
        tags: String? = null,
    ): List<EntrySpec> {
        return listOf(

            EntrySpec(bridgeAssetAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(bridgeLiabilityAccount, amount, EntryType.CREDIT, layer, detail),

            EntrySpec(bridgeLiabilityAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(creditAccount, amount, EntryType.CREDIT, layer, detail, tags)
        )
    }

    private fun createDebitAccountPendingEntries(
        amount: BigDecimal,
        layer: Short,
        detail: String,
        bridgeLiabilityAccount: FinalAccount,
        creditAccount: FinalAccount,
        tags: String? = null,
    ): List<EntrySpec> {
        return listOf(
            // For debit-type credit accounts (like cash), we debit them to increase balance
            EntrySpec(creditAccount, amount, EntryType.DEBIT, layer, detail, tags),
            EntrySpec(bridgeLiabilityAccount, amount, EntryType.CREDIT, layer, detail),
        )
    }

    override fun createLimitEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        // Don't create limit entries for pending transactions as they are already on pending layer
        return emptyList()
    }

    override fun completeTransaction(
        originalTransaction: GLTransaction,
        completionTransaction: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        // For bill payments, revert everything on offset layers
        reverseAllOffsetLayers(originalTransaction, completionTransaction, currenciesLayer)

        // Complete tagged credits for main transaction
        completeTaggedCredits(originalTransaction, completionTransaction, currenciesLayer)
    }

    private fun reverseAllOffsetLayers(
        transaction: GLTransaction,
        completion: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        // Get ALL offset layers including FEE layer
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
                    ?.any { it.contains("credit") || it.contains("debit") } == true
            }

        creditTaggedEntries.forEach { offsetEntry ->

            val currencyLayer = currenciesLayer[offsetEntry.account.currencyCode] ?: 0

            // For bill payments, find the actual recipient account
            val recipientCode = offsetEntry.tags.toString().split(",")
                .first { it.startsWith("credit:") || it.startsWith("debit:") }
                .split(":")[1].trim()

            val type =
                offsetEntry.tags.toString().split(",").firstOrNull { it.startsWith("type:") }?.split(":")[1]?.trim()
                    ?: throw RuntimeException("Unable to complete bill payment")

            transaction.entries.firstOrNull { entry ->
                entry.account.code == recipientCode
            }?.let { recipient ->

                if (setOf(MiniglConstants.EntryTypes.REBATE, MiniglConstants.EntryTypes.COMMISSION).contains(type)) {

                    val baseLayerEntry = completion.createDebit(
                        recipient.account,
                        offsetEntry.amount,
                        "Bill payment completion: ${offsetEntry.detail}",
                        currencyLayer
                    )
                    baseLayerEntry.tags = completion.tags

                    val bridgeEntry = completion.createCredit(
                        offsetEntry.account,
                        offsetEntry.amount,
                        "Bridge debit for bill payment",
                        currencyLayer
                    )
                    bridgeEntry.tags = completion.tags
                } else if (type == MiniglConstants.EntryTypes.AMOUNT) {
                    val bridgeEntry = completion.createDebit(
                        offsetEntry.account,
                        offsetEntry.amount,
                        "Bridge debit for bill payment",
                        currencyLayer
                    )

                    bridgeEntry.tags = completion.tags

                    val baseLayerEntry = completion.createCredit(
                        recipient.account,
                        offsetEntry.amount,
                        "Bill payment completion: ${offsetEntry.detail}",
                        currencyLayer
                    )
                    baseLayerEntry.tags = completion.tags
                }
            }

        }
    }
}