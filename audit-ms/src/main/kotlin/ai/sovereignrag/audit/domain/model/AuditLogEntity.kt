package ai.sovereignrag.audit.domain.model

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLogEntity(

    @Id
    val id: UUID = UUID.randomUUID(),

    val actorId: String,

    val actorName: String,

    val merchantId: String,

    val resource: String,

    val identityType: String,

    val event: String,

    val eventTime: Instant,

    val timeRecorded: Instant,

    @Convert(converter = MapToJsonConverter::class)
    val payload: Map<String, Any>,

    val ipAddress: String = "N/A"
) {

    fun toDomain() = AuditLog(
        id, actorId, actorName, merchantId, resource, identityType, event,
        eventTime,
        timeRecorded,
        payload,
        ipAddress
    )

    companion object {

        const val SORT_COLUMN = "eventTime"

        fun fromDomain(auditLog: AuditLog): AuditLogEntity {
            return AuditLogEntity(
                auditLog.id,
                auditLog.actorId,
                auditLog.actorName,
                auditLog.merchantId,
                auditLog.resource,
                auditLog.identityType,
                auditLog.event,
                auditLog.eventTime,
                auditLog.timeRecorded,
                auditLog.payload,
                auditLog.ipAddress
            )
        }
    }
}


