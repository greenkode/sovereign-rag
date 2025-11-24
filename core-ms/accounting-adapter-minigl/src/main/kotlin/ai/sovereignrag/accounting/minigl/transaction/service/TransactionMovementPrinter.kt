package ai.sovereignrag.accounting.minigl.transaction.service

import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.math.BigDecimal
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

class TransactionMovementPrinter(private val print: Boolean = false) {

    val log = logger {}

    fun printTransactionMovements(transaction: GLTransaction, operation: String = "Created") {

        if (!print) return

        val output = StringBuilder()

        output.appendLine("\n--- Transaction $operation: ${transaction.detail} ---")

        // Add transaction metadata
        val isReversal = transaction.entries.any { it.amount < BigDecimal.ZERO }
        val isCompletion = transaction.tags?.contains("completes:") == true

        if (isReversal) {
            output.appendLine("TYPE: REVERSAL")
        }
        if (isCompletion) {
            val completedRef = transaction.tags?.toString()?.split(",")
                ?.find { it.startsWith("completes:") }
                ?.substringAfter("completes:")
            output.appendLine("COMPLETES: $completedRef")
        }

        // Group entries by layer
        val entriesByLayer = transaction.entries.groupBy { it.layer }
        val currency = transaction.entries.firstOrNull()?.account?.currencyCode ?: MiniglConstants.DEFAULT_CHART_NAME

        // Process each layer
        entriesByLayer.keys.sorted().forEach { layer ->
            val layerName = getLayerName(layer.toInt(), currency)
            output.appendLine()
            output.appendLine("$layerName (L$layer):")

            val layerEntries = entriesByLayer[layer] ?: emptyList()

            // Show all entries if we can't match them into movements
            val movements = groupIntoMovements(layerEntries)

            if (movements.isEmpty() && layerEntries.isNotEmpty()) {
                // Show unmatched entries
                layerEntries.forEach { entry ->
                    appendSingleEntry(output, entry)
                }
            } else {
                movements.forEach { movement ->
                    appendCompactMovement(output, movement)
                }
            }

            // Layer summary
            val totalDebits = layerEntries.filter { !it.isCredit }.sumOf { it.amount.abs() }
            val totalCredits = layerEntries.filter { it.isCredit }.sumOf { it.amount.abs() }
            output.appendLine(
                "  Balance: Dr ${formatAmount(totalDebits, currency)} | Cr ${
                    formatAmount(
                        totalCredits,
                        currency
                    )
                }"
            )
        }

        appendCompactSummary(output, transaction)

        log.info { output.toString() }
    }

    private fun getLayerName(layer: Int, currency: String): String {
        // Assuming NGN has currency ID 566 based on the data
        val baseLayer = 566 // This should be dynamically determined from currency

        return when {
            layer == baseLayer -> "Base"
            layer == baseLayer + 1000 -> "Pending"
            layer == baseLayer + 2000 -> "Credit Allowances"
            layer == baseLayer + 3000 -> "On Hold"
            layer == baseLayer + 4000 -> "Daily Limit"
            layer == baseLayer + 5000 -> "Cumulative Limit"
            layer == baseLayer + 6000 -> "Fee"
            else -> "Layer $layer"
        }
    }


    private fun groupIntoMovements(entries: List<GLEntry>): List<Movement> {
        val movements = mutableListOf<Movement>()
        val credits = entries.filter { it.isCredit }.toMutableList()
        val debits = entries.filter { !it.isCredit }.toMutableList()

        // Match debits with credits based on amount and detail
        debits.forEach { debit ->
            val matchingCredit = credits.find { credit ->
                credit.amount.abs() == debit.amount.abs() && credit.detail == debit.detail
            }

            if (matchingCredit != null) {
                movements.add(
                    Movement(
                        from = debit.account as FinalAccount,
                        to = matchingCredit.account as FinalAccount,
                        amount = debit.amount,
                        detail = debit.detail,
                        tags = debit.tags?.toString() ?: "",
                        isReversal = debit.amount < BigDecimal.ZERO
                    )
                )
                credits.remove(matchingCredit)
            }
        }

        return movements
    }

    private fun appendSingleEntry(output: StringBuilder, entry: GLEntry) {
        val account = getCompactAccountName(entry.account as FinalAccount)
        val amount = formatAmount(entry.amount, entry.account.currencyCode)
        val entryType = if (entry.isCredit) "CR" else "DR"
        val tags = entry.tags?.toString() ?: ""

        val tagInfo = if (tags.isNotEmpty()) " [Tags: $tags]" else ""
        output.appendLine("  $entryType $account | $amount$tagInfo")
    }

    private fun appendCompactMovement(output: StringBuilder, movement: Movement) {
        val fromName = getCompactAccountName(movement.from)
        val toName = getCompactAccountName(movement.to)
        val amount = formatAmount(movement.amount.abs(), movement.from.currencyCode)

        // Check for special tags
        val tagInfo = when {
            movement.tags.contains("credit:") -> " [CREDIT TAGGED]"
            movement.tags.contains("debit:") -> " [DEBIT TAGGED]"
            movement.tags.contains("reversed:true") -> " [REVERSED]"
            movement.tags.contains("completed:true") -> " [COMPLETED]"
            else -> ""
        }

        output.appendLine("  DR $fromName -> CR $toName | $amount$tagInfo")

        // Add detail line if it's meaningful
        if (movement.detail.isNotBlank() && !movement.detail.contains(
                MiniglConstants.DEFAULT_CHART_NAME,
                ignoreCase = true
            )
        ) {
            output.appendLine("    Detail: ${movement.detail}")
        }
    }

    private fun getCompactAccountName(account: FinalAccount): String {
        // Extract account name from tags
        val tagsStr = account.tags?.toString() ?: ""
        val accountName = tagsStr.split(",")
            .find { it.startsWith("account_name:") }
            ?.substringAfter("account_name:")
            ?: account.description

        // Extract account type from tags
        val accountType = tagsStr.split(",")
            .find { it.startsWith("type:") }
            ?.substringAfter("type:")
            ?: ""

        // Determine bridge type from description
        val bridgePrefix = when {
            account.description.startsWith("bridge-assets-") -> "[BridgeAsset]"
            account.description.startsWith("bridge-liabilities-") -> "[BridgeLiab]"
            else -> ""
        }

        // Build compact name
        val parts = mutableListOf<String>()
        if (bridgePrefix.isNotEmpty()) parts.add(bridgePrefix)
        parts.add(accountName)
        if (accountType.isNotEmpty()) parts.add("[$accountType]")
        parts.add("(${account.code})")

        return parts.joinToString(" ")
    }

    private fun getAccountDescription(account: FinalAccount): String {
        val accountName = account.tags?.toString()?.split(",")
            ?.find { it.startsWith("account_name:") }
            ?.substringAfter("account_name:")
            ?: account.description

        return "${account.code} - $accountName (${account.description})"
    }

    private fun formatAmount(amount: BigDecimal, currency: String): String {
        val formatted = String.format("%,.2f", amount)
        return "$currency $formatted"
    }

    private fun appendCompactSummary(output: StringBuilder, transaction: GLTransaction) {
        output.appendLine()
        output.appendLine("--- Summary ---")

        // Count different entry types
        val totalEntries = transaction.entries.size
        val creditTagged = transaction.entries.count { entry ->
            entry.tags?.toString()?.contains("credit:") == true
        }
        val debitTagged = transaction.entries.count { entry ->
            entry.tags?.toString()?.contains("debit:") == true
        }
        val commissionEntries = transaction.entries.count { entry ->
            entry.detail.contains("COMMISSION", ignoreCase = true)
        }
        val rebateEntries = transaction.entries.count { entry ->
            entry.detail.contains("REBATE", ignoreCase = true)
        }
        val feeEntries = transaction.entries.count { entry ->
            entry.detail.contains("FEE", ignoreCase = true) && !entry.detail.contains("REBATE", ignoreCase = true)
        }

        output.appendLine("Total entries: $totalEntries")

        if (creditTagged > 0) {
            output.appendLine("Credit tagged for completion: $creditTagged")
        }
        if (debitTagged > 0) {
            output.appendLine("Debit tagged for completion: $debitTagged")
        }
        if (commissionEntries > 0) {
            output.appendLine("Commission entries: $commissionEntries")
        }
        if (rebateEntries > 0) {
            output.appendLine("Rebate entries: $rebateEntries")
        }
        if (feeEntries > 0) {
            output.appendLine("Fee entries: $feeEntries")
        }

        // Transaction status
        val status = when {
            transaction.tags?.contains("reversed:true") == true -> "REVERSED"
            transaction.tags?.contains("completed:true") == true -> "COMPLETED"
            transaction.tags?.contains("completes:") == true -> "COMPLETION TRANSACTION"
            creditTagged > 0 || debitTagged > 0 -> "PENDING COMPLETION"
            else -> "ACTIVE"
        }

        output.appendLine("Status: $status")
    }

    private data class Movement(
        val from: FinalAccount,
        val to: FinalAccount,
        val amount: BigDecimal,
        val detail: String,
        val tags: String,
        val isReversal: Boolean = false
    )
}