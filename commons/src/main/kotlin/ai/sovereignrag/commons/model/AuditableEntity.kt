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
    @Version var version: Long = 0,
    @CreatedDate protected var createdDate: Instant? = null,
    @CreatedBy protected var createdBy: String? = "system",
    @LastModifiedDate protected var lastModifiedDate: Instant? = null,
    @LastModifiedBy protected var lastModifiedBy: String? = "system"
) : Serializable
