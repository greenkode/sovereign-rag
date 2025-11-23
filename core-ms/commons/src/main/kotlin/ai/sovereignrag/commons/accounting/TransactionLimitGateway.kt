package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.accounting.dto.TransactionLimitDto
import java.util.UUID
import javax.money.CurrencyUnit

interface TransactionLimitGateway {

    fun findByProfileAndTransactionType(
        profileId: UUID,
        transactionType: TransactionType,
        currency: CurrencyUnit
    ): TransactionLimitDto?
}