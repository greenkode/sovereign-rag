package ai.sovereignrag.identity.core.organization.entity

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
class Organization(
    @Id
    val id: UUID = UUID.randomUUID(),
    var name: String,
    var slug: String,
    @Enumerated(EnumType.STRING)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE,
    @Enumerated(EnumType.STRING)
    var plan: SubscriptionTier = SubscriptionTier.TRIAL,
    var databaseName: String? = null,
    var databaseCreated: Boolean = false,
    var maxKnowledgeBases: Int = 5,
    @JdbcTypeCode(SqlTypes.JSON)
    var settings: Map<String, Any> = emptyMap(),
    var updatedAt: Instant = Instant.now()
) : AuditableEntity() {

    constructor() : this(
        id = UUID.randomUUID(),
        name = "",
        slug = ""
    )

    fun generateDatabaseName(): String = "org_$slug"
}
