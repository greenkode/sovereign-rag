package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.gateway.api.request.TransactionEntryRequest
import ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies.DirectTransactionStrategy
import ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies.PendingBillPaymentTransactionStrategy
import ai.sovereignrag.accounting.minigl.transaction.service.builder.strategies.PendingInboundTransactionStrategy
import org.springframework.stereotype.Component

@Component
class TransactionEntryStrategyFactory {

    private val strategies = listOf(
        DirectTransactionStrategy(),
        PendingInboundTransactionStrategy(),
        PendingBillPaymentTransactionStrategy()
    )

    fun getStrategy(request: TransactionEntryRequest, context: TransactionContext): TransactionEntryStrategy {
        return strategies.firstOrNull { it.canHandle(context) }
            ?: throw RuntimeException("No strategy found for request: $request with context: $context")
    }
    
    fun getStrategy(context: TransactionContext): TransactionEntryStrategy {
        return strategies.firstOrNull { it.canHandle(context) }
            ?: // Default to PendingInboundTransactionStrategy for backward compatibility
            PendingInboundTransactionStrategy()
    }
}