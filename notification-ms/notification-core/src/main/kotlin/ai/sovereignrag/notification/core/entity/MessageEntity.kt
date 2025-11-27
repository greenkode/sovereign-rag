package ai.sovereignrag.notification.core.entity

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.TemplateName
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
@Table(name = "messages", schema = "notification")
class MessageEntity() : AuditableEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var publicId: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    var channel: MessageChannel = MessageChannel.EMAIL

    @ManyToOne
    var template: MessageTemplateEntity? = null

    @Enumerated(EnumType.STRING)
    var templateName: TemplateName = TemplateName.WELCOME

    var recipient: String = ""

    var recipientName: String? = null

    @Enumerated(EnumType.STRING)
    var deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING

    var request: String? = null

    var response: String? = null

    @Enumerated(EnumType.STRING)
    var priority: MessagePriority = MessagePriority.NORMAL

    var clientIdentifier: String = ""

    var locale: Locale = Locale.ENGLISH

    var deliveryDate: Instant? = null

    var sentMessageId: String? = null
}
