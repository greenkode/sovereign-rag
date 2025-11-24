package ai.sovereignrag.accounting.transaction.domain.model

import ai.sovereignrag.commons.accounting.TransactionType
import java.util.UUID

interface TransactionReferenceProjection {
    fun getDisplayRef(): String
    fun getTransactionType(): TransactionType
    fun getInternalReference(): UUID
}