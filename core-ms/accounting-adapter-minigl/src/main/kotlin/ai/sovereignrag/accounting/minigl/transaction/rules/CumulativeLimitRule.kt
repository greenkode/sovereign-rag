package ai.sovereignrag.accounting.minigl.transaction.rules

import ai.sovereignrag.accounting.entity.GLAccountEntity as Account
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount
import ai.sovereignrag.accounting.entity.GLEntryEntity as GLEntry
import ai.sovereignrag.accounting.GLException
import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.accounting.entity.GLTransactionEntity as GLTransaction
import ai.sovereignrag.accounting.rule.FinalBalance
import ai.sovereignrag.accounting.minigl.transaction.service.CUMULATIVE_LIMIT_OFFSET
import ai.sovereignrag.commons.enumeration.ResponseCode
import java.math.BigDecimal

/**
 * Cumulative limit rule that enforces cumulative transaction limits by checking balances
 * on the cumulative limit layer (CUMULATIVE_LIMIT_OFFSET + currency_id).
 * 
 * Extends FinalBalance for efficient balance checking and validation.
 */
class CumulativeLimitRule : FinalBalance() {

    override fun getRuleName(): String = "CumulativeLimitRule"

    override fun isError(balance: BigDecimal, previousBalance: BigDecimal): Boolean = true

    override fun check(
        session: GLSession?, 
        txn: GLTransaction,
        param: String?, 
        account: Account?, 
        entryOffsets: IntArray?, 
        layers: ShortArray
    ) {
        // Extract limit from transaction tags
        val limit = extractLimitFromTags(txn) ?: return // No limit specified, skip check
        
        // Now check our cumulative limit logic
        entryOffsets?.forEach { offset ->
            val entry = txn.entries[offset] as GLEntry
            if (shouldCheckEntry(entry, layers)) {
                checkCumulativeLimit(session, txn, entry, limit, entryOffsets, layers)
            }
        }
    }

    private fun checkCumulativeLimit(
        session: GLSession?,
        txn: GLTransaction,
        entry: GLEntry,
        limit: BigDecimal,
        entryOffsets: IntArray?,
        layers: ShortArray
    ) {
        val journal = txn.journal
        val account = entry.account as? FinalAccount ?: return
        
        val targetLayer = calculateTargetLayer(account)
        if (!layers.contains(targetLayer)) return
        
        try {
            val currentBalance = session?.getBalance(journal, account, shortArrayOf(targetLayer)) ?: BigDecimal.ZERO
            val impact = getImpact(txn, account, entryOffsets, targetLayer)
            val projectedBalance = currentBalance.add(impact)
            
            if (projectedBalance.compareTo(limit) > 0) {
                throw GLException(
                    "Cumulative limit of $limit exceeded for account ${account.code}. " +
                    "Current balance: $currentBalance, " +
                    "Transaction impact: $impact, " +
                    "Projected balance: $projectedBalance", ResponseCode.TRANSACTION_LIMIT_EXCEEDED
                )
            }
        } catch (e: GLException) {
            throw e
        } catch (e: Exception) {
            throw GLException("Error checking cumulative limit for account ${account.code}: ${e.message}", ResponseCode.TRANSACTION_LIMIT_EXCEEDED)
        }
    }

    private fun getImpact(
        txn: GLTransaction,
        account: Account,
        entryOffsets: IntArray?,
        targetLayer: Short
    ): BigDecimal {
        var impact = BigDecimal.ZERO
        
        entryOffsets?.forEach { offset ->
            val entry = txn.entries[offset] as GLEntry
            if (account == entry.account && entry.layer == targetLayer) {
                impact = impact.add(entry.impact)
            }
        }
        
        return impact
    }

    private fun shouldCheckEntry(entry: GLEntry, layers: ShortArray): Boolean {
        return layers.any { layer -> isRelevantLayer(entry.layer, layer) }
    }

    private fun calculateTargetLayer(account: FinalAccount): Short {
        val currencyId = account.currencyCode.toShortOrNull() ?: 0
        return (CUMULATIVE_LIMIT_OFFSET + currencyId).toShort()
    }

    private fun isRelevantLayer(entryLayer: Short, configuredLayer: Short): Boolean {
        return entryLayer >= CUMULATIVE_LIMIT_OFFSET && 
               entryLayer < (CUMULATIVE_LIMIT_OFFSET + 1000) &&
               entryLayer == configuredLayer
    }

    private fun extractLimitFromTags(txn: GLTransaction): BigDecimal? {
        val tags = txn.tags?.toString() ?: return null
        
        return tags.split(",")
            .map { it.split(":") }
            .find { it.size == 2 && it[0].trim() == "cumulative_limit" }
            ?.let { 
                try {
                    BigDecimal(it[1].trim())
                } catch (e: NumberFormatException) {
                    throw GLException("Invalid cumulative limit value: ${it[1].trim()}", ResponseCode.TRANSACTION_LIMIT_EXCEEDED)
                }
            }
    }
}