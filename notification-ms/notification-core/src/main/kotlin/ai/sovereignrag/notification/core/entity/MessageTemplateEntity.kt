package ai.sovereignrag.notification.core.entity

import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Locale

@Entity
@Table(name = "message_templates", schema = "notification")
class MessageTemplateEntity() : AuditableEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    var name: TemplateName = TemplateName.WELCOME

    @Enumerated(EnumType.STRING)
    var channel: MessageChannel = MessageChannel.EMAIL

    var title: String = ""

    var content: String? = null

    var externalId: String? = null

    var locale: Locale = Locale.ENGLISH

    @Enumerated(EnumType.STRING)
    var recipientType: RecipientType = RecipientType.INDIVIDUAL

    var active: Boolean = true

    constructor(
        name: TemplateName,
        channel: MessageChannel,
        title: String,
        content: String? = null,
        externalId: String? = null,
        locale: Locale = Locale.ENGLISH,
        recipientType: RecipientType = RecipientType.INDIVIDUAL
    ) : this() {
        this.name = name
        this.channel = channel
        this.title = title
        this.content = content
        this.externalId = externalId
        this.locale = locale
        this.recipientType = recipientType
    }
}
