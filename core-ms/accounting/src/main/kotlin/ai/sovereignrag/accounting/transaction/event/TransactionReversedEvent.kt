package ai.sovereignrag.accounting.transaction.event

import ai.sovereignrag.accounting.transaction.domain.Transaction
import ai.sovereignrag.commons.accounting.AccountDto

data class TransactionReversedEvent(
    val transaction: Transaction,
    val senderAccount: AccountDto,
    val recipientAccount: AccountDto
)