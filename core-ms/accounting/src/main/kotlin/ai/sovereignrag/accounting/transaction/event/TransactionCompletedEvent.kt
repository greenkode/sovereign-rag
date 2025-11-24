package ai.sovereignrag.accounting.transaction.event

import ai.sovereignrag.accounting.transaction.domain.Transaction
import ai.sovereignrag.commons.accounting.AccountDto
import java.util.UUID

data class TransactionCompletedEvent(
    val transaction: Transaction,
    val senderAccount: AccountDto,
    val recipientAccount: AccountDto,
    val merchantId: UUID
)