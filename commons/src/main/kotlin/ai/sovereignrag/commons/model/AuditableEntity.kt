package ai.sovereignrag.commons.model

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class AuditableEntity(
    @Version open var version: Long = 0,
    @CreatedDate open var createdAt: Instant? = null,
    @CreatedBy open var createdBy: String? = "system",
    @LastModifiedDate open var lastModifiedAt: Instant? = null,
    @LastModifiedBy open var lastModifiedBy: String? = "system"
) : Serializable