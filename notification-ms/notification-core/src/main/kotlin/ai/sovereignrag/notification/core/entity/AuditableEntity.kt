package ai.sovereignrag.notification.core.entity

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

@MappedSuperclass
abstract class AuditableEntity {
    var createdAt: Instant? = null
    var createdBy: String? = null
    var lastModifiedAt: Instant? = null
    var lastModifiedBy: String? = null

    @Version
    var version: Long = 0
}
