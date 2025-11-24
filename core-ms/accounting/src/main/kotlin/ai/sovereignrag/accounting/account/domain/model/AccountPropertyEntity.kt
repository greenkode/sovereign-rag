package ai.sovereignrag.accounting.account.domain.model

import ai.sovereignrag.accounting.account.domain.AccountProperty
import ai.sovereignrag.commons.accounting.AccountPropertyName
import ai.sovereignrag.commons.accounting.AccountPropertyScope
import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account_property")
class AccountPropertyEntity(

    @Enumerated(EnumType.STRING)
    val name: AccountPropertyName,

    @Enumerated(EnumType.STRING)
    val scope: AccountPropertyScope,

    val scopeValue: String,

    val value: String,

    @ManyToOne
    val account: AccountEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : AuditableEntity() {

    fun toDomain() = AccountProperty(id!!, name, scope, scopeValue, value)
}