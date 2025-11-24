package ai.sovereignrag.commons.banking

import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction
import org.javamoney.moneta.Money
import java.util.UUID
import javax.money.MonetaryAmount

data class BankingTransactionStatusResult(
    val externalReference: String,
    val internalReference: UUID,
    val action: TransactionFollowUpAction,
    val destination: String,
    val destinationName: String,
    val amount: MonetaryAmount
)