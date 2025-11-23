package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.exception.PropertyNotFoundException
import ai.sovereignrag.commons.notification.enumeration.NotificationChannel
import ai.sovereignrag.commons.notification.enumeration.NotificationParameter
import ai.sovereignrag.commons.notification.enumeration.NotificationPriority
import ai.sovereignrag.commons.notification.enumeration.NotificationType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import java.io.Serializable
import java.util.Locale
import java.util.UUID


data class NotificationEventPayload(
    val recipients: List<NotificationRecipient>,
    val templateName: TemplateName,
    val channel: NotificationChannel,
    val priority: NotificationPriority,
    val type: NotificationType,
    val parameters: Map<NotificationParameter, String>,
    val locale: Locale,
    val userId: String? = null,
    val processId: UUID? = null,
    val accountType: AccountType? = null
) : Serializable {

    fun requiredParameterValue(parameter: NotificationParameter): String {
        return this.parameters[parameter] ?: throw PropertyNotFoundException(parameter.name)
    }
}
