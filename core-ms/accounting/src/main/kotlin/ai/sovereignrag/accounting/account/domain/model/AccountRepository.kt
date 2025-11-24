package ai.sovereignrag.accounting.account.domain.model

import ai.sovereignrag.commons.accounting.AccountDto
import ai.sovereignrag.commons.accounting.AccountType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.money.CurrencyUnit

@Repository
interface AccountRepository : JpaRepository<AccountEntity, Long> {

    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        WHERE a.publicId = :publicId
    """)
    fun findByPublicId(@Param("publicId") publicId: UUID): AccountEntity?

    fun findByAddresses_Address(address: String): AccountEntity?

    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        JOIN a.addresses addr 
        WHERE addr.address IN :addresses
    """)
    fun findAllByAddresses_AddressIn(@Param("addresses") addresses: Set<String>): List<AccountEntity>

    fun existsByUserId(userId: UUID): Boolean

    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        WHERE a.userId = :userId AND a.currency = :currency AND a.type = :type
    """)
    fun findFirstByUserIdAndCurrencyAndType(@Param("userId") userId: UUID, @Param("currency") currency: CurrencyUnit, @Param("type") type: AccountType): AccountEntity?

    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        WHERE a.parentAccountId = :parentId AND a.type = :type
    """)
    fun findByParentAccountIdAndType(@Param("parentId") id: Long, @Param("type") type: AccountType): AccountEntity?
    
    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        WHERE a.parentAccountId = :parentId AND a.type IN :types
    """)
    fun findByParentAccountIdAndTypeIn(@Param("parentId") id: Long, @Param("types") types: Set<AccountType>): List<AccountEntity>

    @Query("""
        SELECT a FROM AccountEntity a 
        LEFT JOIN FETCH a.profile 
        WHERE a.userId = :userId
    """)
    fun findAllByUserId(@Param("userId") userId: UUID): List<AccountEntity>

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<AccountEntity>

    fun findByUserIdAndPublicId(userId: UUID, publicId: UUID): AccountEntity?

    fun findFirstByUserIdAndCurrency(userId: UUID, currency: CurrencyUnit): AccountEntity?

    fun findFirstByUserIdAndTypeAndIsDefaultIsTrue(userId: UUID, type: AccountType): AccountEntity?

    fun findByTypeAndCurrency(type: AccountType, currency: CurrencyUnit): AccountEntity?

    fun findAllByUserIdAndTypeIn(userId: UUID, accountTypes: List<AccountType>, pageable: Pageable): Page<AccountEntity>

    fun findAllByPublicIdIn(publicIds: Set<UUID>) : List<AccountEntity>

    @Query("""
        SELECT DISTINCT a.address FROM AccountAddressEntity a
         WHERE a.type = ai.sovereignrag.commons.accounting.AccountAddressType.CHART_OF_ACCOUNTS AND a.account.publicId IN (:publicIds)
    """)
    fun findCoaAddressesByPublicIds(publicIds: Set<UUID>) : List<String>

    fun findAllByType(type: AccountType, pageable: Pageable): Page<AccountEntity>
}