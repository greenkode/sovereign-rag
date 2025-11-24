package ai.sovereignrag.accounting.transaction.domain.model

import ai.sovereignrag.commons.accounting.TransactionPropertyGroup
import ai.sovereignrag.commons.accounting.TransactionPropertyName


data class TransactionProperty(val name: TransactionPropertyName, val value: String, val group: TransactionPropertyGroup)