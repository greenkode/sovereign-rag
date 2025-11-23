package ai.sovereignrag.process.dao

import ai.sovereignrag.commons.property.SystemPropertyName
import ai.sovereignrag.commons.property.SystemPropertyScope
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemPropertyRepository : JpaRepository<SystemPropertyEntity, Long> {

    fun findByNameAndScope(name: SystemPropertyName, scope: SystemPropertyScope): SystemPropertyEntity?
}