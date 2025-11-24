package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.CurrencyEntity as Currency
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount

data class TransactionContext(
    val isPending: Boolean,
    val chart: CompositeAccount,
    val currencies: Map<String, Currency>,
    val accounts: Map<String, FinalAccount>,
    val bridgeAccounts: Map<String, FinalAccount>,
    val group: String,
    val type: String
)

enum class TransactionType {
    DIRECT,
    PENDING,
    REVERSAL,
    COMPLETION
}

enum class LayerType(val offset: Short) {
    BASE(0),
    PENDING(1000),
    DAILY_LIMIT(4000),
    CUMULATIVE_LIMIT(5000),
    ON_HOLD(3000),
    CREDIT_ALLOWANCES(2000),
    FEE(6000)
}

data class EntrySpec(
    val account: FinalAccount,
    val amount: java.math.BigDecimal,
    val type: EntryType,
    val layer: Short,
    val detail: String,
    val tags: String? = null
)

enum class EntryType {
    DEBIT,
    CREDIT
}