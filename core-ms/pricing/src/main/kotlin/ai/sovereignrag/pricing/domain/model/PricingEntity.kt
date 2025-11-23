package ai.sovereignrag.pricing.domain.model

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.currency.CurrencyUnitConverter
import ai.sovereignrag.commons.pricing.dto.PricingCalculation
import ai.sovereignrag.commons.pricing.dto.PricingDataDto
import ai.sovereignrag.commons.pricing.dto.PricingDto
import ai.sovereignrag.commons.pricing.dto.PricingType
import ai.sovereignrag.pricing.domain.Pricing
import ai.sovereignrag.pricing.domain.PricingCalculationFactory
import ai.sovereignrag.pricing.domain.PricingData
import ai.sovereignrag.pricing.domain.calculation.CalculationContext
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit

@Entity
@Table(name = "pricing", schema = "core")
class PricingEntity(

    val publicId: UUID,

    @Enumerated(EnumType.STRING)
    val accountType: AccountType,

    val accountPublicId: UUID?,

    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,

    @Convert(converter = CurrencyUnitConverter::class)
    val currency: CurrencyUnit,

    val validFrom: Instant,

    val productId: UUID?,

    val validUntil: Instant?,

    val integratorId: String? = null,

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "pricing")
    val data: Set<PricingDataEntity> = mutableSetOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity() {

    fun toDomain() = Pricing(
        id!!,
        publicId,
        productId,
        accountType,
        accountPublicId,
        transactionType,
        currency,
        validFrom,
        validUntil,
        integratorId,
        data.map { it.toDomain() }.toSet()
    )

    fun toDto(billPayProductId: UUID?, productName: String?) = PricingDto(
        id = id!!,
        publicId = publicId,
        vendorId = productId,
        productId = billPayProductId,
        productName = productName,
        accountType = accountType,
        accountPublicId = accountPublicId,
        transactionType = transactionType,
        currency = currency,
        validFrom = validFrom,
        validUntil = validUntil,
        integratorId = integratorId,
        data = data.map { it.toDto() }.toSet()
    )
}

@Entity
@Table(name = "pricing_data", schema = "core")
class PricingDataEntity(

    @Enumerated(EnumType.STRING)
    val pricingType: PricingType,

    @Enumerated(EnumType.STRING)
    val calculation: PricingCalculation,

    val value: BigDecimal,

    val expression: String?,

    @ManyToOne
    val pricing: PricingEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity() {

    fun calculate(amount: BigDecimal): BigDecimal {
        return PricingCalculationFactory.getCalculator(calculation)
            .calculate(CalculationContext(this.toDomain(), amount))
    }

    fun toDomain() = PricingData(id!!, pricingType, calculation, value, expression)

    fun toDto() = PricingDataDto(
        pricingType = pricingType,
        calculation = calculation,
        value = value,
        expression = expression
    )
}