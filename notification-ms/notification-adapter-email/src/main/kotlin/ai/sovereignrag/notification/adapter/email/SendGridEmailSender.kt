package ai.sovereignrag.notification.adapter.email

import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.notification.core.entity.MessageTemplateEntity
import ai.sovereignrag.notification.core.service.EmailSender
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SendGridEmailSender(
    @Value("\${sendgrid.api-key:}")
    private val apiKey: String,
    @Value("\${sendgrid.from-email:noreply@sovereignrag.ai}")
    private val fromEmail: String,
    @Value("\${sendgrid.from-name:Sovereign RAG}")
    private val fromName: String
) : EmailSender {

    override fun sendEmail(
        recipients: List<MessageRecipient>,
        template: MessageTemplateEntity,
        parameters: Map<String, String>
    ) {
        apiKey.takeIf { it.isBlank() }?.let {
            log.warn { "SendGrid API key not configured, skipping email send" }
            return
        }

        val sendGrid = SendGrid(apiKey)
        val from = Email(fromEmail, fromName)

        val content = template.content?.let { processTemplate(it, parameters) } ?: ""
        val mail = Mail()
        mail.setFrom(from)
        mail.setSubject(processTemplate(template.title, parameters))
        mail.addContent(Content("text/html", content))

        val personalization = Personalization()
        recipients.forEach { recipient ->
            personalization.addTo(Email(recipient.address, recipient.name))
        }
        parameters.forEach { (key, value) ->
            personalization.addDynamicTemplateData(key, value)
        }
        mail.addPersonalization(personalization)

        template.externalId?.let { mail.templateId = it }

        val request = Request().apply {
            method = Method.POST
            endpoint = "mail/send"
            body = mail.build()
        }

        runCatching {
            val response = sendGrid.api(request)
            log.info { "SendGrid response: statusCode=${response.statusCode}" }
            response.statusCode.takeIf { it >= 400 }?.let {
                throw RuntimeException("SendGrid error: ${response.body}")
            }
        }.onFailure { e ->
            log.error(e) { "Failed to send email via SendGrid" }
            throw e
        }
    }

    private fun processTemplate(template: String, parameters: Map<String, String>): String {
        var result = template
        parameters.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
            result = result.replace("\${$key}", value)
        }
        return result
    }
}
