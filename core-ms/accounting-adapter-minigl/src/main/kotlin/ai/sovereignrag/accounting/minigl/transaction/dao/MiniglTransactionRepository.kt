package ai.sovereignrag.accounting.minigl.transaction.dao

import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.accounting.Tags
import ai.sovereignrag.accounting.repository.GLTransactionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction
import ai.sovereignrag.accounting.entity.GLTransactionGroupEntity as GLTransactionGroup

@Repository
class MiniglTransactionRepository(private val glSession: GLSession, private val glTransactionRepository: GLTransactionRepository) {

    private val log = KotlinLogging.logger {}

    fun postTransaction(transaction: GLTransaction) {
        glSession.post(transaction.journal, transaction)
    }

    fun findByReference(reference: String): GLTransaction? {
        val optional = glTransactionRepository.findByDetail(reference)
        return if (optional.isPresent) optional.get() else null
    }

    fun reverseTransaction(transaction: GLTransaction, reversalReference: String): String {

        val reversal = transaction.createReverse(true)
        reversal.timestamp = Instant.now()
        reversal.postDate = Instant.now()
        reversal.detail = reversalReference

        reversal.tags = Tags(transaction.tags.toString())
        reversal.tags.add("reverses:${transaction.detail}")

        glSession.post(reversal.journal, reversal)

        transaction.tags.add("reversed:true")
        transaction.tags.add("reversal_reference:${reversal.detail}")

        return reversal.detail
    }

    fun completeTransaction(transaction: GLTransaction, completion: GLTransaction) {

        if (completion.journal == null) {
            completion.journal = transaction.journal
        }

        glSession.post(completion.journal, completion)

        glSession.createGroup(transaction.detail, mutableListOf(transaction, completion))

        transaction.tags.add("completed:true")
        transaction.tags.add("completion_reference:${completion.detail}")
    }

    fun findTransactionGroup(detail: String): GLTransactionGroup? {
        return glSession.findTransactionGroup(detail)
    }

    fun getMaxTransactionEntryId(transactionReference: String): Long {
        return findTransactionGroup(transactionReference)
            ?.let { transactionGroup ->
                transactionGroup.transactions
                    .flatMap { it.entries }
                    .maxByOrNull { it.id }?.id
            } ?: findByReference(transactionReference)
            ?.entries?.maxByOrNull { it.id }?.id
            ?: throw RuntimeException("Unable to find transaction group or transaction for reference: $transactionReference")
    }
}