package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.Tags
import org.springframework.stereotype.Component
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction

@Component
class EntrySpecExecutor {
    
    fun executeSpecs(transaction: GLTransaction, specs: List<EntrySpec>): List<GLEntry> {
        return specs.map { spec ->
            when (spec.type) {
                EntryType.DEBIT -> transaction.createDebit(spec.account, spec.amount, spec.detail, spec.layer)
                EntryType.CREDIT -> transaction.createCredit(spec.account, spec.amount, spec.detail, spec.layer)
            }.also { entry ->
                // Apply entry-specific tags if provided, otherwise use transaction tags
                if (spec.tags != null) {
                    // Combine transaction tags with entry-specific tags
                    val transactionTags = transaction.tags?.toString() ?: ""
                    val combinedTags = if (transactionTags.isNotEmpty()) {
                        "$transactionTags,${spec.tags}"
                    } else {
                        spec.tags
                    }
                    entry.tags = Tags(combinedTags)
                } else {
                    entry.tags = transaction.tags
                }
            }
        }
    }
}

// Extension function for convenient execution
fun List<EntrySpec>.execute(transaction: GLTransaction): List<GLEntry> {
    return EntrySpecExecutor().executeSpecs(transaction, this)
}

// DSL-style execution
fun GLTransaction.executeEntries(specs: List<EntrySpec>): List<GLEntry> {
    return specs.execute(this)
}