package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.gateway.api.request.TransactionEntryRequest
import ai.sovereignrag.accounting.entity.CurrencyEntity as Currency
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

data class EntryBuilderPayload(
    val transaction: GLTransaction,
    val entry: TransactionEntryRequest,
    val layer: Currency,
    val debitAccount: FinalAccount,
    val creditAccount: FinalAccount,
    val bridgeAssetAccount: FinalAccount?,
    val bridgeLiabilityAccount: FinalAccount? = null
)