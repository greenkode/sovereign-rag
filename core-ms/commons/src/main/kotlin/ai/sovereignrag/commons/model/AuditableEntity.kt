package ai.sovereignrag.commons.model

import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class AuditableEntity {
    var createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()
}
