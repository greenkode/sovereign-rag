package ai.sovereignrag.license.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customer", schema = "license")
data class Customer(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val customerId: String,

    val customerName: String,

    val email: String,

    val companyName: String? = null,

    val contactPerson: String? = null,

    val phone: String? = null,

    val address: String? = null,

    val country: String? = null,

    val status: CustomerStatus = CustomerStatus.ACTIVE,

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now(),

    val createdBy: String = "system",

    val updatedBy: String = "system"
)

enum class CustomerStatus {
    ACTIVE,
    SUSPENDED,
    INACTIVE,
    TRIAL
}
