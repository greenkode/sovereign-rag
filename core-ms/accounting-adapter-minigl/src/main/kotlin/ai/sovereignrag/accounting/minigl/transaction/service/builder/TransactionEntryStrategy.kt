package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.entity.CurrencyEntity
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

interface TransactionEntryStrategy {
    fun canHandle(context: TransactionContext): Boolean
    fun createEntries(payload: EntryBuilderPayload): List<EntrySpec>
    fun completeTransaction(
        originalTransaction: GLTransaction,
        completionTransaction: GLTransaction,
        currenciesLayer: Map<String, Short>
    )
}

abstract class BaseEntryStrategy : TransactionEntryStrategy {
    
    final override fun createEntries(payload: EntryBuilderPayload): List<EntrySpec> {
        return buildList {
            addAll(createBaseLayerEntries(payload))
            addAll(createOffsetLayerEntries(payload))
            addAll(createLimitEntries(payload))
        }
    }
    
    protected abstract fun createBaseLayerEntries(payload: EntryBuilderPayload): List<EntrySpec>
    protected abstract fun createOffsetLayerEntries(payload: EntryBuilderPayload): List<EntrySpec>
    protected open fun createLimitEntries(payload: EntryBuilderPayload): List<EntrySpec> = emptyList()
    
    // Default implementation for completion - can be overridden by subclasses
    override fun completeTransaction(
        originalTransaction: GLTransaction,
        completionTransaction: GLTransaction,
        currenciesLayer: Map<String, Short>
    ) {
        // Default behavior - no custom completion logic
        // Subclasses can override this to provide custom completion behavior
    }
    
    protected fun getBaseLayer(currency: CurrencyEntity): Short = currency.id.toShort()
    protected fun getPendingLayer(currency: CurrencyEntity): Short = 
        (LayerType.PENDING.offset.toInt() + currency.id.toInt()).toShort()
    protected fun getDailyLimitLayer(currency: CurrencyEntity): Short = 
        (LayerType.DAILY_LIMIT.offset.toInt() + currency.id.toInt()).toShort()
    protected fun getCumulativeLimitLayer(currency: CurrencyEntity): Short = 
        (LayerType.CUMULATIVE_LIMIT.offset.toInt() + currency.id.toInt()).toShort()
    protected fun getCommissionRebateFeeLayer(currency: CurrencyEntity): Short = 
        (LayerType.FEE.offset.toInt() + currency.id.toInt()).toShort()
}