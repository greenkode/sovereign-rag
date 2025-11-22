package ai.sovereignrag.messaging.sendgrid

import ai.sovereignrag.commons.messaging.dto.MessagePayload
import ai.sovereignrag.commons.messaging.dto.MessageSentResult
import ai.sovereignrag.commons.messaging.dto.MessageTemplateDto
import ai.sovereignrag.commons.messaging.enumeration.DeliveryStatus
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

const val SENDGRID_EMAIL_INTEGRATION = "SENDGRID_EMAIL"

@Component(SENDGRID_EMAIL_INTEGRATION)
class SendGridMessagingIntegration(
    @Value("\${messaging.sendgrid.api-key}") private val apiKey: String,
    @Value("\${messaging.sendgrid.from-address}") private val fromAddress: String,
    @Value("\${messaging.sendgrid.from-name}") private val fromName: String,
    @Value("\${messaging.sendgrid.reply-to-address:}") private val replyToAddress: String?
) {

    fun getId(): String = SENDGRID_EMAIL_INTEGRATION

    fun sendMessage(
        payload: MessagePayload,
        template: MessageTemplateDto
    ): MessageSentResult {
        return try {
            val sg = SendGrid(apiKey)
            val mail = Mail()
            mail.setTemplateId(template.externalId)
            mail.setFrom(Email(fromAddress, fromName))

            // Build email with recipients and template parameters
            payload.recipient.let { recipient ->
                val personalization = Personalization()
                personalization.addTo(Email(recipient.address, recipient.name))
                payload.parameters.forEach { (key, value) ->
                    personalization.addDynamicTemplateData(key, value)
                }
                mail.addPersonalization(personalization)
            }

            // Add reply-to if configured
            replyToAddress?.takeIf { it.isNotBlank() }?.let {
                mail.setReplyTo(Email(it))
            }

            val request = Request()
            request.method = Method.POST
            request.endpoint = "/mail/send"
            request.body = mail.build()

            val response = sg.api(request)

            logger.info { "SendGrid API response: status=${response.statusCode}, body=${response.body}" }

            MessageSentResult(
                if (response.statusCode in 200..299)
                    DeliveryStatus.DELIVERED
                else
                    DeliveryStatus.PENDING,
                response.body
            )
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to send email via SendGrid" }
            MessageSentResult(DeliveryStatus.FAILED, ex.message)
        }
    }
}
