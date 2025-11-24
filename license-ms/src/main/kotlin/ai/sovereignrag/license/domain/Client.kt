package ai.sovereignrag.license.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "client", schema = "license")
data class Client(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val clientId: String,

    val clientName: String,

    val email: String,

    val companyName: String? = null,

    val contactPerson: String? = null,

    val phone: String? = null,

    val address: String? = null,

    val country: String? = null,

    @Enumerated(EnumType.STRING)
    val status: ClientStatus = ClientStatus.ACTIVE,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now(),

    val createdBy: String = "system",

    val updatedBy: String = "system"
)

enum class ClientStatus {
    ACTIVE,
    SUSPENDED,
    INACTIVE,
    TRIAL
}
