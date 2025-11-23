package ai.sovereignrag.audit.domain.model

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditLogRepository : CrudRepository<AuditLogEntity, UUID> {

    fun findAllByMerchantId(
        merchantId: String,
        pageable: Pageable
    ): Page<AuditLogEntity>
}