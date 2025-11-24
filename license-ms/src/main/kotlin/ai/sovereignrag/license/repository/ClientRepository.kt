package ai.sovereignrag.license.repository

import ai.sovereignrag.license.domain.Client
import ai.sovereignrag.license.domain.ClientStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClientRepository : JpaRepository<Client, UUID> {

    fun findByClientId(clientId: String): Client?

    fun findByEmail(email: String): Client?

    fun findByStatus(status: ClientStatus): List<Client>

    fun findByClientIdIn(clientIds: List<String>): List<Client>
}
