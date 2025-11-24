package ai.sovereignrag.accounting.account.domain.model

import ai.sovereignrag.accounting.account.domain.AccountAddress
import ai.sovereignrag.accounting.account.domain.AddressProperty
import ai.sovereignrag.commons.accounting.AccountAddressPropertyName
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.currency.CurrencyUnitConverter
import jakarta.persistence.CollectionTable
import jakarta.persistence.Convert
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import javax.money.CurrencyUnit

@Entity
@Table(name = "account_address")
class AccountAddressEntity(

    val address: String,

    @Enumerated(EnumType.STRING)
    val type: AccountAddressType,

    val platform: String,

    @Convert(converter = CurrencyUnitConverter::class)
    val currency: CurrencyUnit,

    @ManyToOne
    val account: AccountEntity,

    @BatchSize(size = 20)
    @ElementCollection
    @CollectionTable(name = "account_address_property", joinColumns = [JoinColumn(name = "account_address_id")])
    val properties: Set<AddressPropertyEmbeddable> = mutableSetOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : AuditableEntity() {

    fun toDomain() = AccountAddress(
        address,
        type,
        platform,
        currency,
        properties.map { it.toDomain() }.toSet()
    )

}


@Embeddable
data class AddressPropertyEmbeddable(
    @Enumerated(EnumType.STRING)
    val name: AccountAddressPropertyName,
    val value: String,
) {

    fun toDomain(): AddressProperty {
        return AddressProperty(name, value)
    }

}