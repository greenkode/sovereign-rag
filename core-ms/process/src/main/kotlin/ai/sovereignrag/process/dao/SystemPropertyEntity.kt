package ai.sovereignrag.process.dao

import ai.sovereignrag.process.domain.model.SystemProperty
import ai.sovereignrag.commons.property.SystemPropertyScope
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.property.SystemPropertyName
import ai.sovereignrag.commons.util.Constants.Companion.DATA_LOADING_ERROR
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "system_property")
data class SystemPropertyEntity(

    @Enumerated(EnumType.STRING)
    val name: SystemPropertyName,

    @Enumerated(EnumType.STRING)
    val scope: SystemPropertyScope,

    var value: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    ) : AuditableEntity() {

    fun toDomain() = SystemProperty(
        id ?: throw IllegalArgumentException(DATA_LOADING_ERROR),
        name, scope, value
    )
}