package ai.sovereignrag.accounting.minigl.transaction.service

import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import java.math.BigDecimal

data class EntryParams(
    val account: FinalAccount,
    val amount: BigDecimal,
    val detail: String,
    val layerId: Short
)