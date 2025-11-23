package ai.sovereignrag.notification.infrastructure

import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.notification.enumeration.TemplateRecipientType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Locale

@Repository
interface MessageTemplateRepository : JpaRepository<MessageTemplateEntity, Long> {

    fun findByNameAndLocaleAndChannelAndRecipientTypeAndActiveIsTrue(
        name: TemplateName, locale: Locale,
        channel: MessageChannel, recipientType: TemplateRecipientType
    ): MessageTemplateEntity?
}