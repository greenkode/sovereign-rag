package ai.sovereignrag.accounting.account.domain.model

import ai.sovereignrag.accounting.account.domain.AccountProfile
import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.util.Constants.Companion.DATA_LOADING_ERROR
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "account_profile")
class AccountProfileEntity(

    val name: String,

    val description: String,

    val publicId: UUID,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null

) : AuditableEntity() {
    fun toDomain() = AccountProfile(
        id ?: throw InvalidRequestException(DATA_LOADING_ERROR),
        name,
        description,
        publicId
    )
}