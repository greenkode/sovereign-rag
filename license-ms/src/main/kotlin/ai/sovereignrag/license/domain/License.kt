package ai.sovereignrag.license.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "license", schema = "license")
data class License(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    val licenseKey: String,

    val clientId: String,

    @Enumerated(EnumType.STRING)
    val tier: LicenseTier,

    val maxTokensPerMonth: Long,

    val maxTenants: Int,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    val features: Array<String>,

    val issuedAt: Instant = Instant.now(),

    val expiresAt: Instant? = null,

    val revokedAt: Instant? = null,

    val revokedBy: String? = null,

    val revocationReason: String? = null,

    @Enumerated(EnumType.STRING)
    val status: LicenseStatus = LicenseStatus.ACTIVE,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val metadata: Map<String, Any>? = null,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now(),

    val createdBy: String = "system",

    val updatedBy: String = "system"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as License

        if (id != other.id) return false
        if (licenseKey != other.licenseKey) return false
        if (clientId != other.clientId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + licenseKey.hashCode()
        result = 31 * result + clientId.hashCode()
        return result
    }
}

enum class LicenseTier {
    TRIAL,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE,
    UNLIMITED
}

enum class LicenseStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    SUSPENDED
}

enum class LicenseFeature {
    MULTI_TENANT,
    READ_REPLICAS,
    CUSTOM_MODELS,
    ADVANCED_GUARDRAILS,
    PRIORITY_SUPPORT,
    CUSTOM_INTEGRATIONS,
    WHITE_LABEL,
    ON_PREMISE,
    HIGH_AVAILABILITY,
    ADVANCED_ANALYTICS
}
