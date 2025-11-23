package ai.sovereignrag.notification

import ai.sovereignrag.commons.notification.dto.MessagePayload
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.dto.MessageSentResult
import ai.sovereignrag.commons.notification.dto.MessageTemplateDto
import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Attachments
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import java.io.IOException

const val SEND_GRID_EMAIL = "SEND_GRID_EMAIL"

@Component(SEND_GRID_EMAIL)
class SendGridEmailNotificationIntegration(@Value("\${integration.sendgrid.api-key}") private val apiKey: String,
    @Value("\${integration.sendgrid.from-address}") private val fromAddress: String,
    @Value("\${integration.sendgrid.from-name}") private val fromName: String,
    @Value("\${integration.sendgrid.reply-to-address}") private val replyToAddress: String,) :
    NotificationIntegration {

    val log = KotlinLogging.logger {}

    override fun getId(): String {
        return SEND_GRID_EMAIL
    }

    override fun sendMessage(
        payload: MessagePayload,
        template: MessageTemplateDto
    ): MessageSentResult {
        return sendMessage(payload, template, sendInCc = false)
    }

    fun sendMessage(
        recipients: List<MessageRecipient>,
        template: MessageTemplateDto,
        parameters: Map<String, Any>,
        sendInCc: Boolean = false
    ): MessageSentResult {

        try {
            if (recipients.isEmpty()) {
                return MessageSentResult(DeliveryStatus.FAILED, "No recipients provided")
            }

            val sg = SendGrid(apiKey)
            val request = Request()
            request.method = Method.POST
            request.endpoint = "/mail/send"

            val mail = Mail()
            mail.setTemplateId(template.externalId)
            mail.setFrom(Email(fromAddress, fromName))
            mail.setReplyTo(Email(replyToAddress, fromName))

            if (sendInCc && recipients.size > 1) {
                // Send to all recipients in a single email where everyone can see each other
                val personalization = Personalization()
                
                // Add first recipient as "To"
                personalization.addTo(Email(recipients.first().address, recipients.first().name))
                
                // Add remaining recipients as "CC"
                recipients.drop(1).forEach { recipient ->
                    personalization.addCc(Email(recipient.address, recipient.name))
                }
                
                // Add template parameters
                parameters.forEach { (key, value) ->
                    personalization.addDynamicTemplateData(key, value)
                }
                
                mail.addPersonalization(personalization)
                
                log.info { "Sending email to ${recipients.size} recipients in CC mode" }
            } else {
                // Send individual emails to each recipient (current behavior)
                recipients.forEach { recipient ->
                    val personalization = Personalization()
                    personalization.addTo(Email(recipient.address, recipient.name))
                    parameters.forEach { (key, value) ->
                        personalization.addDynamicTemplateData(key, value)
                    }
                    mail.addPersonalization(personalization)
                }
                
                log.info { "Sending individual emails to ${recipients.size} recipients" }
            }

            // Check for attachment in parameters
            val attachmentContent = parameters["attachment"] as? String
            val filename = parameters["filename"] as? String
            if (attachmentContent != null && filename != null) {
                val attachment = Attachments()
                attachment.content = attachmentContent
                attachment.type = parameters["content_type"] as? String
                attachment.filename = filename
                attachment.disposition = "attachment"
                mail.addAttachments(attachment)
                log.info { "Added attachment: $filename" }
            }

            request.body = mail.build()
            val response = sg.api(request)

            return MessageSentResult(
                if (HttpStatusCode.valueOf(response.statusCode).is2xxSuccessful) DeliveryStatus.DELIVERED else DeliveryStatus.PENDING,
                response.body,
            )
        } catch (ex: IOException) {
            log.error(ex) { ex.message }
            return MessageSentResult(DeliveryStatus.FAILED, ExceptionUtils.getStackTrace(ex))
        }
    }

    fun sendMessage(
        payload: MessagePayload,
        template: MessageTemplateDto,
        sendInCc: Boolean = false
    ): MessageSentResult {
        return sendMessage(
            recipients = listOf(payload.recipient),
            template = template,
            parameters = payload.parameters,
            sendInCc = sendInCc
        )
    }
}