package ai.sovereignrag.accounting.account.domain.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountProfileRepository : JpaRepository<AccountProfileEntity, Long> {

    fun findByName(name: String): AccountProfileEntity?
}