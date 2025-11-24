package ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies

import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import ai.sovereignrag.accounting.minigl.transaction.service.builder.BaseEntryStrategy
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryBuilderPayload
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntrySpec
import ai.sovereignrag.accounting.minigl.transaction.service.builder.EntryType
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionContext
import java.math.BigDecimal

class DirectTransactionStrategy : BaseEntryStrategy() {
    
    override fun canHandle(context: TransactionContext): Boolean {
        return !context.isPending
    }
    
    override fun createBaseLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        val amount = payload.entry.amount.number.numberValue(BigDecimal::class.java)
        val layer = getBaseLayer(payload.layer)
        val detail = payload.entry.detail
        
        return listOf(
            EntrySpec(payload.debitAccount, amount, EntryType.DEBIT, layer, detail),
            EntrySpec(payload.creditAccount, amount, EntryType.CREDIT, layer, detail)
        )
    }
    
    override fun createOffsetLayerEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        // Direct transactions don't need offset layers
        return emptyList()
    }
    
    override fun createLimitEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        if (payload.entry.metadata["type"] == MiniglConstants.EntryTypes.AMOUNT &&
            payload.entry.metadata["skip_limits"]?.toBoolean() != true) {
            
            // Don't create limit entries for bridge account debits
            if (payload.debitAccount.description.startsWith("bridge")) {
                return emptyList()
            }
            
            val amount = payload.entry.amount.number.numberValue(BigDecimal::class.java)
            val dailyLayer = getDailyLimitLayer(payload.layer)
            val cumulativeLayer = getCumulativeLimitLayer(payload.layer)
            val detail = "transaction_limit"
            
            val bridgeAssetAccount = payload.bridgeAssetAccount
                ?: return emptyList() // No bridge account available
            
            return listOf(
                // Daily limit entries
                EntrySpec(bridgeAssetAccount, amount, EntryType.DEBIT, dailyLayer, detail),
                EntrySpec(payload.debitAccount, amount, EntryType.CREDIT, dailyLayer, detail),
                
                // Cumulative limit entries
                EntrySpec(bridgeAssetAccount, amount, EntryType.DEBIT, cumulativeLayer, detail),
                EntrySpec(payload.debitAccount, amount, EntryType.CREDIT, cumulativeLayer, detail)
            )
        }
        return emptyList()
    }
}