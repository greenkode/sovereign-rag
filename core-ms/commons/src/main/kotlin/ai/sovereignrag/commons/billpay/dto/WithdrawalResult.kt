package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction
import java.util.UUID

abstract class WithdrawalResult {

    abstract val internalReference: UUID

    abstract val externalReference: String

    abstract val action: TransactionFollowUpAction
}

data class DefaultWithdrawalResult(
    override val internalReference: UUID,
    override val externalReference: String,
    override val action: TransactionFollowUpAction
) : WithdrawalResult()