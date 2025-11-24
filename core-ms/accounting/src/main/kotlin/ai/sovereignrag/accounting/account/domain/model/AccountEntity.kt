package ai.sovereignrag.accounting.account.domain.model

import ai.sovereignrag.accounting.account.domain.Account
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.AccountStatus
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.kyc.TrustLevel
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.currency.CurrencyUnitConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.javamoney.moneta.Money
import java.math.BigDecimal
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount


@Entity
@Table(name = "account")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class AccountEntity(

    val name: String,

    val alias: String,

    val publicId: UUID,

    val userId: UUID,

    balanceSnapshot: MonetaryAmount,

    @Enumerated(EnumType.STRING)
    val type: AccountType,

    @Enumerated(EnumType.STRING)
    val trustLevel: TrustLevel,

    @Enumerated(EnumType.STRING)
    var status: AccountStatus,

    @ManyToOne
    val profile: AccountProfileEntity,

    val isDefault: Boolean = false,

    val parentAccountId: Long? = null,

    @BatchSize(size = 20)
    @OneToMany(mappedBy = "account", cascade = [CascadeType.ALL])
    val addresses: MutableSet<AccountAddressEntity> = mutableSetOf(),

    @BatchSize(size = 20)
    @OneToMany(mappedBy = "account", cascade = [CascadeType.ALL])
    val properties: MutableSet<AccountPropertyEntity> = mutableSetOf(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : AuditableEntity() {

    @Convert(converter = CurrencyUnitConverter::class)
    val currency: CurrencyUnit = balanceSnapshot.currency

    fun toDomain(): Account {
        return Account(
            id!!,
            name,
            alias,
            publicId,
            trustLevel,
            userId,
            currency,
            type,
            status,
            isDefault,
            profile.toDomain(),
            addresses.map { it.toDomain() }.toSet(),
            properties.associateBy({ it.name }, { it.value })
        )
    }

    fun addAddress(address: AccountAddressEntity) {
        addresses.add(address)
    }

    fun coaCode() = addresses.first { it.type == AccountAddressType.CHART_OF_ACCOUNTS }.address
}