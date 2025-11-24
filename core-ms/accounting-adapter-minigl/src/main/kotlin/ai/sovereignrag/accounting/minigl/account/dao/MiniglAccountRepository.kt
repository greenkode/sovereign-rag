package ai.sovereignrag.accounting.minigl.account.dao

import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.accounting.entity.GLAccountEntity
import ai.sovereignrag.accounting.entity.CompositeAccountEntity
import ai.sovereignrag.accounting.entity.CurrencyEntity
import ai.sovereignrag.accounting.entity.FinalAccountEntity
import ai.sovereignrag.accounting.entity.JournalEntity
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.javamoney.moneta.Money
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.money.MonetaryAmount

@Repository
class MiniglAccountRepository(
    @Qualifier("accountingEntityManagerFactory") val entityManagerFactory: EntityManagerFactory,
    val glSession: GLSession,
    @Qualifier("accountingEntityManager") val entityManager: EntityManager,
    private val runningBalanceRepository: RunningBalanceRepository
) {

    private val log = KotlinLogging.logger {}

    @LogExecutionTime
    fun addAccountFast(parent: CompositeAccountEntity, account: GLAccountEntity) {
        glSession.addAccount(parent, account)
    }

    fun getCompositeAccountByChartAndCode(chart: GLAccountEntity, code: String): CompositeAccountEntity? {
        val query = entityManager.createQuery(
            "FROM CompositeAccountEntity WHERE root = :chart AND code = :code",
            CompositeAccountEntity::class.java
        )
        query.setParameter("chart", chart)
        query.setParameter("code", code)
        return query.resultList.firstOrNull()
    }

    fun getChartByDescription(description: String): CompositeAccountEntity? {
        val query = entityManager.createQuery(
            "FROM CompositeAccountEntity WHERE description = :description",
            CompositeAccountEntity::class.java
        )
        query.setParameter("description", description)
        return query.resultList.firstOrNull()
    }

    fun getBalance(journal: JournalEntity, account: GLAccountEntity, currency: CurrencyEntity): MonetaryAmount {
        return Money.of(glSession.getBalance(journal, account, currency.id), currency.name)
    }

    fun getAccountWithChartDescriptionAndCurrency(chart: CompositeAccountEntity, desc: String, currency: String): GLAccountEntity? {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE root = :chart AND description = :description AND currencyCode = :currency",
            GLAccountEntity::class.java
        )
        query.setParameter("chart", chart)
        query.setParameter("description", desc)
        query.setParameter("currency", currency)
        return query.resultList.firstOrNull()
    }

    fun getAccountByCode(chart: CompositeAccountEntity, code: String): GLAccountEntity? {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE root = :chart AND code = :code",
            GLAccountEntity::class.java
        )
        query.setParameter("chart", chart)
        query.setParameter("code", code)
        return query.resultList.firstOrNull()
    }

    fun findFinalAccountsByCodesIn(ids: Set<String>): List<FinalAccountEntity> {
        if (ids.isEmpty()) return emptyList()
        val query = entityManager.createQuery(
            "FROM FinalAccountEntity WHERE code IN :codes",
            FinalAccountEntity::class.java
        )
        query.setParameter("codes", ids)
        return query.resultList
    }

    fun findAccountsByCodesIn(ids: Set<String>): List<GLAccountEntity> {
        if (ids.isEmpty()) return emptyList()
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE code IN :codes",
            GLAccountEntity::class.java
        )
        query.setParameter("codes", ids)
        return query.resultList
    }

    @LogExecutionTime
    fun getCompositeAccountByChartAndDescription(chart: CompositeAccountEntity, description: String): CompositeAccountEntity? {
        val query = entityManager.createQuery(
            "FROM CompositeAccountEntity WHERE root = :chart AND description = :description",
            CompositeAccountEntity::class.java
        )
        query.setParameter("chart", chart)
        query.setParameter("description", description)
        return query.resultList.firstOrNull()
    }

    fun getFinalAccountByChartAndDescription(root: CompositeAccountEntity, description: String): FinalAccountEntity? {
        val query = entityManager.createQuery(
            "FROM FinalAccountEntity WHERE root = :root AND description = :description",
            FinalAccountEntity::class.java
        )
        query.setParameter("root", root)
        query.setParameter("description", description)
        return query.resultList.firstOrNull()
    }

    fun getFinalAccountsByChartAndDescriptionIn(root: CompositeAccountEntity, descriptions: Set<String>): List<FinalAccountEntity> {
        if (descriptions.isEmpty()) return emptyList()
        val query = entityManager.createQuery(
            "FROM FinalAccountEntity WHERE root = :root AND description IN :descriptions",
            FinalAccountEntity::class.java
        )
        query.setParameter("root", root)
        query.setParameter("descriptions", descriptions)
        return query.resultList
    }

    fun getChildrenAccounts(parent: CompositeAccountEntity): List<GLAccountEntity> {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE parent = :parent ORDER BY code",
            GLAccountEntity::class.java
        )
        query.setParameter("parent", parent)
        return query.resultList
    }

    fun getChildrenAccountsPaginated(parent: CompositeAccountEntity, page: Int, pageSize: Int, currency: String): Pair<List<GLAccountEntity>, Long> {
        // Get total count with currency filter
        val countQuery = entityManager.createQuery(
            "SELECT COUNT(acct) FROM GLAccountEntity acct WHERE acct.parent = :parent AND acct.currencyCode = :currency",
            java.lang.Long::class.java
        )
        countQuery.setParameter("parent", parent)
        countQuery.setParameter("currency", currency)
        val totalCount = countQuery.singleResult?.toLong() ?: 0L

        // Get paginated results with a currency filter (not cached due to pagination)
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE parent = :parent AND currencyCode = :currency ORDER BY code",
            GLAccountEntity::class.java
        )
        query.setParameter("parent", parent)
        query.setParameter("currency", currency)
        query.firstResult = page * pageSize
        query.maxResults = pageSize

        return Pair(query.resultList, totalCount)
    }

    fun getDistinctCurrencyCodesInChart(chart: CompositeAccountEntity): Set<String> {
        val query = entityManager.createQuery(
            "SELECT DISTINCT acct.currencyCode FROM GLAccountEntity acct WHERE acct.root = :chart AND acct.currencyCode IS NOT NULL",
            String::class.java
        )
        query.setParameter("chart", chart)
        return query.resultList.toSet()
    }

    fun getMaxChildCodeSuffix(parent: GLAccountEntity): Long {
        val children = getChildrenAccounts(parent as CompositeAccountEntity)
        val parentCodeLength = parent.code.length

        return children.mapNotNull { child ->
            val childCode = child.code
            if (childCode.startsWith(parent.code) && childCode.length > parentCodeLength) {
                val suffix = childCode.substring(parentCodeLength)
                suffix.toLongOrNull()
            } else null
        }.maxOrNull() ?: 0L
    }

    @LogExecutionTime
    fun getAllCharts(): List<CompositeAccountEntity> {
        val query = entityManager.createQuery(
            "FROM CompositeAccountEntity WHERE parent IS NULL",
            CompositeAccountEntity::class.java
        )
        return query.resultList
    }

    fun getDefaultChart(): CompositeAccountEntity? {
        return getAllCharts().firstOrNull()
    }

    fun getAccountByCode(code: String): GLAccountEntity? {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE code = :code",
            GLAccountEntity::class.java
        )
        query.setParameter("code", code)
        return query.resultList.firstOrNull()
    }

    fun getAccountsByCurrency(currency: String, pageable: Pageable): List<GLAccountEntity> {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE currencyCode = :currency ORDER BY code",
            GLAccountEntity::class.java
        )
        query.setParameter("currency", currency)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize
        return query.resultList
    }

    fun findAccountsByUserId(userId: String): List<GLAccountEntity> {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE description = :userId OR tags LIKE :userIdPattern",
            GLAccountEntity::class.java
        )
        query.setParameter("userId", userId)
        query.setParameter("userIdPattern", "%user:$userId%")
        return query.resultList
    }

    fun getAccountsByType(type: String, pageable: Pageable): List<GLAccountEntity> {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE type = :type ORDER BY code",
            GLAccountEntity::class.java
        )
        query.setParameter("type", type)
        query.firstResult = pageable.pageNumber * pageable.pageSize
        query.maxResults = pageable.pageSize
        return query.resultList
    }

    fun getAccountById(id: Long): GLAccountEntity? {
        val query = entityManager.createQuery(
            "FROM GLAccountEntity WHERE id = :id",
            GLAccountEntity::class.java
        )
        query.setParameter("id", id)
        return query.resultList.firstOrNull()
    }

    fun getRunningBalanceAsOfEntry(
        journal: JournalEntity,
        account: GLAccountEntity,
        currency: CurrencyEntity,
        maxId: Long
    ): MonetaryAmount {
        return Money.of(
            runningBalanceRepository.getBalance(journal, account, null, currency.id.toString(), maxId),
            currency.name
        )
    }
}