package ai.sovereignrag.commons.coa

import java.time.Instant
import java.time.LocalDate


data class ChartOfAccountsRequest(
    val currencies: List<CurrencyRequest> = emptyList(),
    val chartOfAccounts: ChartOfAccounts,
    val journals: List<JournalRequest> = emptyList(),
    val strictAccountsCode: Boolean = true
)

data class ChartOfAccounts(
    val code: String,
    val created: Instant,
    val description: String,
    val currency: String,
    val compositeAccounts: MutableList<CompositeAccountRequest> = mutableListOf()
)

data class CompositeAccountRequest(
    val code: String,
    val type: String,
    val description: String,
    val currency: String,
    val accounts: MutableList<FinalAccountRequest> = mutableListOf(),
    val compositeAccounts: MutableList<CompositeAccountRequest> = mutableListOf()
)

data class CurrencyRequest(
    val id: String,
    val symbol: String,
    val name: String
)

data class FinalAccountRequest(
    val code: String,
    val type: String,
    val description: String
)


data class Grant(
    val user: String,
    val permission: String
)

data class LayerRequest(
    val id: Short,
    val name: String
)


data class JournalRequest(
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
    val status: String,
    val chart: String,
    val grants: MutableList<Grant> = mutableListOf(),
    val rules: MutableList<RuleRequest> = mutableListOf(),
    val layers: MutableList<LayerRequest> = mutableListOf()
)

data class RuleRequest(
    val clazz: String,
    val layers: String?,
    val param: String? = null,
    val description: String
)


data class UserRequest(
    val nick: String,
    val name: String,
    val grants: MutableList<String> = mutableListOf()
)