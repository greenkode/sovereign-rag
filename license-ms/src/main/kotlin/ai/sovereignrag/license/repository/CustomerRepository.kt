package ai.sovereignrag.license.repository

import ai.sovereignrag.license.domain.Customer
import ai.sovereignrag.license.domain.CustomerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {

    fun findByCustomerId(customerId: String): Customer?

    fun findByEmail(email: String): Customer?

    fun findByStatus(status: CustomerStatus): List<Customer>

    fun findByCustomerIdIn(customerIds: List<String>): List<Customer>
}
