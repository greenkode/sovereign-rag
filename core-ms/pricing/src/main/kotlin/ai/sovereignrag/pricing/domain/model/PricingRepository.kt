package ai.sovereignrag.pricing.domain.model

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface PricingRepository : JpaRepository<PricingEntity, Long> {

    fun findByPublicId(publicId: UUID): PricingEntity?

    fun existsByPublicId(publicId: UUID): Boolean

    @Query("""
        SELECT p FROM PricingEntity p
        WHERE p.accountType = :accountType
        AND p.transactionType = :transactionType
        AND (p.accountPublicId = :accountPublicId OR p.accountPublicId IS NULL)
        AND (p.productId = :productId OR p.productId IS NULL)
        AND (p.integratorId = :integratorId OR p.integratorId IS NULL)
        AND p.validFrom <= :date
        AND (p.validUntil IS NULL OR p.validUntil > :date)
        ORDER BY p.accountPublicId NULLS LAST, p.productId NULLS LAST, p.integratorId NULLS LAST
    """)
    fun findValidPricing(
        accountType: AccountType,
        transactionType: TransactionType,
        accountPublicId: UUID?,
        productId: UUID?,
        integratorId: String?,
        date: Instant,
        page: Pageable
    ): Page<PricingEntity>

    @Query("""
        SELECT p FROM PricingEntity p
        WHERE (:accountType IS NULL OR p.accountType = :accountType)
        AND (:transactionType IS NULL OR p.transactionType = :transactionType)
        AND (:accountPublicId IS NULL OR p.accountPublicId = :accountPublicId)
        AND (:productId IS NULL OR p.productId = :productId)
        AND (:integratorId IS NULL OR p.integratorId = :integratorId)
        ORDER BY p.validFrom DESC
    """)
    fun findByFilters(
        accountType: AccountType?,
        transactionType: TransactionType?,
        accountPublicId: UUID?,
        productId: UUID?,
        integratorId: String?
    ): List<PricingEntity>

    @Query("""
        SELECT p FROM PricingEntity p
        WHERE (:accountType IS NULL OR p.accountType = :accountType)
        AND (:transactionType IS NULL OR p.transactionType = :transactionType)
        AND (:accountPublicId IS NULL OR p.accountPublicId = :accountPublicId)
        AND (:productId IS NULL OR p.productId = :productId)
        AND (:integratorId IS NULL OR p.integratorId = :integratorId)
        AND (p.validUntil IS NULL OR p.validUntil > CURRENT_TIMESTAMP)
        ORDER BY p.validFrom DESC
    """)
    fun findUnexpiredPricingByFilters(
        accountType: AccountType?,
        transactionType: TransactionType?,
        accountPublicId: UUID?,
        productId: UUID?,
        integratorId: String?,
        pageable: Pageable
    ): Page<PricingEntity>

    @Query("""
        SELECT p FROM PricingEntity p
        WHERE p.validFrom <= :activeOn
        AND (p.validUntil IS NULL OR p.validUntil > :activeOn)
        ORDER BY p.validFrom DESC
    """)
    fun findActivePricing(
        activeOn: Instant,
        pageable: Pageable
    ): Page<PricingEntity>
    
    @Query("""
        SELECT p FROM PricingEntity p 
        WHERE p.accountType = :accountType 
        AND p.transactionType = :transactionType 
        AND (p.accountPublicId = :accountPublicId OR (p.accountPublicId IS NULL AND :accountPublicId IS NULL))
        AND (p.productId = :productId OR (p.productId IS NULL AND :productId IS NULL))
        AND (p.integratorId = :integratorId OR (p.integratorId IS NULL AND :integratorId IS NULL))
        AND (
            (p.validFrom <= :validFrom AND (p.validUntil IS NULL OR p.validUntil > :validFrom))
            OR 
            (p.validFrom < :validUntil AND (p.validUntil IS NULL OR p.validUntil > :validFrom))
            OR
            (:validFrom <= p.validFrom AND :validUntil > p.validFrom)
        )
    """)
    fun findOverlappingPricing(
        accountType: AccountType,
        transactionType: TransactionType,
        accountPublicId: UUID?,
        productId: UUID?,
        integratorId: String?,
        validFrom: Instant,
        validUntil: Instant?
    ): List<PricingEntity>
}

@Repository
interface PricingDataRepository : JpaRepository<PricingDataEntity, Long> {
    
    fun findByPricingId(pricingId: Long): List<PricingDataEntity>
}