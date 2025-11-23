package ai.sovereignrag.notification.infrastructure

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.notification.domain.Message
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Entity
@Table(name = "message")
class MessageEntity(

    @Enumerated(EnumType.STRING)
    val channel: MessageChannel,

    @ManyToOne
    val template: MessageTemplateEntity,

    val recipient: String? = null,

    @Enumerated(EnumType.STRING)
    val deliveryStatus: DeliveryStatus,

    val request: String,

    val response: String?,

    val integrator: String,

    @Enumerated(EnumType.STRING)
    val priority: MessagePriority,

    val clientIdentifier: String,

    val locale: Locale,

    val publicId: UUID = UUID.randomUUID(),

    val deliveryDate: Instant? = null,

    val status: String,

    val sentMessageId: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : AuditableEntity() {

    fun toDomain() = Message(
        channel,
        template.name,
        recipient,
        deliveryStatus,
        request,
        response,
        integrator,
        priority,
        clientIdentifier,
        locale,
        status,
        deliveryDate,
        id
    )
}