package ai.sovereignrag.accounting.limit.domain

import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.cache.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit

@Repository
interface TransactionLimitRepository : JpaRepository<TransactionLimitEntity, Int> {

    @Cacheable(value = [CacheNames.ACCOUNT], key = "'tx_limit_' + #profileId.toString() + '_' + #transactionType.name() + '_' + #currency.currencyCode", unless = "#result == null")
    fun findByProfileIdAndTransactionTypeAndCurrencyAndStartBeforeAndExpiryIsNullOrExpiryAfter(
        profileId: UUID,
        transactionType: TransactionType,
        currency: CurrencyUnit,
        start: Instant,
        end: Instant
    ): TransactionLimitEntity?
    
    @CacheEvict(value = [CacheNames.ACCOUNT], allEntries = true)
    override fun <S : TransactionLimitEntity> save(entity: S): S
    
    @CacheEvict(value = [CacheNames.ACCOUNT], allEntries = true)
    override fun <S : TransactionLimitEntity> saveAll(entities: MutableIterable<S>): MutableList<S>
}