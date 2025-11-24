package ai.sovereignrag.accounting.minigl.account.dao

import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.accounting.entity.CurrencyEntity
import ai.sovereignrag.accounting.entity.GLAccountEntity
import ai.sovereignrag.accounting.entity.JournalEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@Repository
class RunningBalanceRepository(
    @Qualifier("accountingEntityManager") private val entityManager: EntityManager,
    private val glSession: GLSession
) {

    private val log = KotlinLogging.logger {}

    fun getBalance(
        journal: JournalEntity,
        account: GLAccountEntity,
        date: Instant?,
        layers: String,
        maxId: Long
    ): BigDecimal {
        return getBalances(journal, account, date, true, toLayers(layers), maxId)[0]
    }

    private fun toLayers(layers: String): ShortArray {
        val tokens = layers.split(",", " ").filter { it.isNotBlank() }
        return tokens.map { it.trim().toShort() }.toShortArray()
    }

    private fun getBalances(
        journal: JournalEntity,
        account: GLAccountEntity,
        date: Instant?,
        inclusive: Boolean,
        layers: ShortArray,
        maxId: Long
    ): Array<BigDecimal> {
        val select = StringBuilder(
            "SELECT SUM(CASE WHEN entry.credit = false THEN entry.amount ELSE -entry.amount END) AS balance, " +
                    "COUNT(entry.id) AS count " +
                    "FROM GLEntryEntity AS entry "
        )

        val balance = arrayOf(BigDecimal.ZERO, BigDecimal.ZERO)
        select.append(", GLTransactionEntity as txn ")

        val accountCache = glSession.getBalanceCache(journal, account, layers)

        if (!account.isFinalAccount) {
            select.append(", GLAccountEntity as acct ")
        }

        val conditions = mutableListOf<String>()

        if (maxId > 0L) {
            conditions.add("entry.id <= :maxId")
        }

        if (accountCache != null && accountCache.ref > 0) {
            conditions.add("entry.id > :bcacheRef")
        }

        if (account.isFinalAccount) {
            conditions.add("entry.account.id = :acctId")
        } else if (account.isChart) {
            conditions.add("entry.account.id = acct.id")
            conditions.add("acct.root.id = :acctId")
        } else {
            conditions.add("entry.account.id = acct.id")
            conditions.add("acct.code LIKE :code")
        }

        conditions.add("entry.transaction.id = txn.id")
        conditions.add("txn.journal.id = :journal")

        if (date != null) {
            conditions.add("txn.postDate <= :date")
        }

        if (layers.isNotEmpty()) {
            conditions.add("entry.layer IN :layers")
        }

        select.append(" WHERE ")
        select.append(conditions.joinToString(" AND "))

        val query = entityManager.createQuery(select.toString())

        if (account.isFinalAccount || account.isChart) {
            query.setParameter("acctId", account.id)
        } else {
            query.setParameter("code", "${account.code}%")
        }

        query.setParameter("journal", journal.id)

        if (date != null) {
            query.setParameter("date", if (inclusive) date.plusSeconds(86400) else date)
        }

        if (maxId > 0L) {
            query.setParameter("maxId", maxId)
        }

        if (accountCache != null && accountCache.ref > 0) {
            query.setParameter("bcacheRef", accountCache.ref)
        }

        if (layers.isNotEmpty()) {
            query.setParameter("layers", layers.toList())
        }

        try {
            val result = query.resultList

            if (result.isNotEmpty()) {
                val row = result[0] as Array<*>
                val bd = row[0] as? BigDecimal
                if (bd != null) {
                    balance[0] = bd
                    val count = row[1]
                    balance[1] = when (count) {
                        is BigInteger -> BigDecimal(count)
                        is Long -> BigDecimal.valueOf(count)
                        is Int -> BigDecimal.valueOf(count.toLong())
                        else -> BigDecimal.ZERO
                    }
                    if (account.isCredit) {
                        balance[0] = balance[0].negate()
                    }
                }
            }

            if (accountCache != null && accountCache.balance != null) {
                balance[0] = balance[0].add(accountCache.balance)
            }
        } catch (e: Exception) {
            log.error(e) { "Error calculating running balance for account ${account.code}: ${e.message}" }
            throw RuntimeException("Unable to calculate running balance", e)
        }

        return balance
    }
}
