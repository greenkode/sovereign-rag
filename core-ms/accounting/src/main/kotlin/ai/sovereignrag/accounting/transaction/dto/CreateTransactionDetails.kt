package ai.sovereignrag.accounting.transaction.dto

import java.util.UUID

data class CreateTransactionDetails(val reference: UUID, val metadata: Map<String, String>)
