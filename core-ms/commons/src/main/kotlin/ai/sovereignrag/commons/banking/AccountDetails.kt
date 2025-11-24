package ai.sovereignrag.commons.banking

import ai.sovereignrag.commons.accounting.AccountAddressPropertyName
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.AccountStatus

data class AccountDetails(
    val address: String,
    val name: String,
    val bankName: String,
    val status: AccountStatus,
    val integratorId: String,
    val accountAddressType: AccountAddressType,
    val properties: Map<AccountAddressPropertyName, String> = mapOf()
)