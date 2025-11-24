package ai.sovereignrag.accounting.gateway.api.request


import ai.sovereignrag.accounting.limit.domain.TransactionLimit
import ai.sovereignrag.commons.accounting.TransactionGroup
import ai.sovereignrag.commons.accounting.TransactionType
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import javax.money.MonetaryAmount

data class CreateTransactionResponse(
    @JsonProperty("reference")
    val reference: UUID,

    @JsonProperty("metadata")
    val metadata: Map<String, String>
)

data class CreateTransactionRequest(
    @JsonProperty("reference")
    val reference: UUID,

    @JsonProperty("type")
    val type: TransactionType,

    @JsonProperty("group")
    val group: TransactionGroup,

    @JsonProperty("pending")
    val pending: Boolean,

    @JsonProperty("limits")
    val limit: TransactionLimit?,

    @JsonProperty("metadata")
    val metadata: Map<String, String>,

    @JsonProperty("entries")
    val entries: List<TransactionEntryRequest>
)

data class TransactionEntryRequest(
    @JsonProperty("detail")
    val detail: String,

    @JsonProperty("amount")
    val amount: MonetaryAmount,

    @JsonProperty("debit_account")
    val debitAccount: String,

    @JsonProperty("credit_account")
    val creditAccount: String,

    @JsonProperty("metadata")
    val metadata: Map<String, String>
)