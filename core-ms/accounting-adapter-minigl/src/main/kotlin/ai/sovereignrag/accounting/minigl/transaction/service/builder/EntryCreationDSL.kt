package ai.sovereignrag.accounting.minigl.transaction.service.builder

import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction
import java.math.BigDecimal

class EntryCreationDSL(private val transaction: GLTransaction) {
    
    fun entry(account: FinalAccount, amount: BigDecimal, layer: Short, detail: String) = 
        EntryBuilder(account, amount, layer, detail)
        
    inner class EntryBuilder(
        private val account: FinalAccount,
        private val amount: BigDecimal, 
        private val layer: Short,
        private val detail: String
    ) {
        fun debit(): GLEntry {
            return transaction.createDebit(account, amount, detail, layer).also {
                it.tags = transaction.tags 
            }
        }
        
        fun credit(): GLEntry {
            return transaction.createCredit(account, amount, detail, layer).also {
                it.tags = transaction.tags 
            }
        }
        
        fun spec(type: EntryType): EntrySpec {
            return EntrySpec(account, amount, type, layer, detail)
        }
    }
}

// Extension function for convenient usage
fun GLTransaction.withEntries(block: EntryCreationDSL.() -> Unit) {
    val dsl = EntryCreationDSL(this)
    dsl.block()
}

// Utility functions for entry creation
fun EntryCreationDSL.debitSpec(account: FinalAccount, amount: BigDecimal, layer: Short, detail: String): EntrySpec {
    return entry(account, amount, layer, detail).spec(EntryType.DEBIT)
}

fun EntryCreationDSL.creditSpec(account: FinalAccount, amount: BigDecimal, layer: Short, detail: String): EntrySpec {
    return entry(account, amount, layer, detail).spec(EntryType.CREDIT)
}