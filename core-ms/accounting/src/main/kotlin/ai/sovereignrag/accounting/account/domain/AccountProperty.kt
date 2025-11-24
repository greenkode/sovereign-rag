package ai.sovereignrag.accounting.account.domain

import ai.sovereignrag.commons.accounting.AccountPropertyName
import ai.sovereignrag.commons.accounting.AccountPropertyScope

data class AccountProperty(
    val id: Long,
    val name: AccountPropertyName,
    val scope: AccountPropertyScope,
    val scopeValue: String,
    val value: String,
)