package ai.sovereignrag.notification.core.repository

import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.notification.core.entity.MessageTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Locale

@Repository
interface MessageTemplateRepository : JpaRepository<MessageTemplateEntity, Long> {
    fun findByNameAndChannelAndLocaleAndRecipientTypeAndActiveIsTrue(
        name: TemplateName,
        channel: MessageChannel,
        locale: Locale,
        recipientType: RecipientType
    ): MessageTemplateEntity?

    fun findByNameAndChannelAndActiveIsTrue(
        name: TemplateName,
        channel: MessageChannel
    ): List<MessageTemplateEntity>

    fun findByActive(active: Boolean): List<MessageTemplateEntity>
}
