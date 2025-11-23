package ai.sovereignrag.commons.accounting

data class TransactionPropertyDto(
    val name: TransactionPropertyName,
    val value: String,
    val group: TransactionPropertyGroup
)