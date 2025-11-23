package ai.sovereignrag.audit.domain.query

import ai.sovereignrag.audit.domain.model.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditLogReadRepository : CrudRepository<AuditLogEntity, UUID> {

    fun findAllByMerchantId(
        merchantId: String,
        pageable: Pageable
    ): Page<AuditLogEntity>
}