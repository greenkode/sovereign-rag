package ai.sovereignrag.accounting.minigl.account.dto

import javax.money.MonetaryAmount

data class AccountDto(
    val code: String, val description: String, val currency: String,
    val created: String, val type: Int, val parent: Long?, val balance: MonetaryAmount)
