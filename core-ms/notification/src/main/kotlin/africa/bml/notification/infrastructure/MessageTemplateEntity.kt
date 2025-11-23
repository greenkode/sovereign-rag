package ai.sovereignrag.notification.infrastructure

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.notification.enumeration.TemplateRecipientType
import ai.sovereignrag.notification.domain.MessageTemplate
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Locale

@Entity
@Table(name = "message_template")
class MessageTemplateEntity(

    @Enumerated(EnumType.STRING)
    val channel: MessageChannel,

    val content: String,

    val title: String,

    @Enumerated(EnumType.STRING)
    val name: TemplateName,

    val locale: Locale,

    val externalId: String,

    val active: Boolean,

    @Enumerated(EnumType.STRING)
    val recipientType: TemplateRecipientType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : AuditableEntity() {

    fun toDomain() = MessageTemplate(channel, content, title, name, locale, externalId, active, recipientType, id!!)
}