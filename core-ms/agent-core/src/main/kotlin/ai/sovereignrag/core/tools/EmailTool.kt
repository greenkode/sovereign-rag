package ai.sovereignrag.core.tools

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import dev.langchain4j.agent.tool.Tool
import mu.KotlinLogging
import nl.compilot.ai.commons.EscalationLogger
import nl.compilot.ai.config.CompilotProperties
import nl.compilot.ai.domain.Escalation
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Email tool that allows the AI agent to send escalation emails to the support team
 */
@Component
class EmailTool(
    private val properties: CompilotProperties,
    private val escalationLogger: EscalationLogger
) {

    private val sendGrid: SendGrid? = if (properties.sendgrid.apiKey.isNotBlank()) {
        SendGrid(properties.sendgrid.apiKey)
    } else {
        logger.warn { "SendGrid API key not configured - email notifications will be disabled" }
        null
    }

    @Tool("""
        Send an escalation email to the support team when a customer needs human assistance.
        Use this tool ONLY after you have collected ALL required information from the user:
        - userEmail: The customer's email address (required, must be valid email format)
        - userName: The customer's full name (required)
        - userMessage: The customer's detailed message or issue description (required)
        - userPhone: The customer's phone number (optional, can be empty string)
        - sessionId: The current chat session ID (you have access to this)

        Before calling this tool, make sure you have asked the user for all required information in a natural conversation.

        Example conversation flow:
        1. Detect user is struggling
        2. Ask: "Would you like me to connect you with a human support agent?"
        3. If yes, ask for email: "What's your email address?"
        4. Ask for name: "What's your name?"
        5. Ask for phone (optional): "What's your phone number? (You can skip this if you prefer)"
        6. Ask for detailed message: "Please describe your issue in detail so our team can help you better"
        7. THEN call this tool with all the collected information

        Returns a confirmation message to show the user.
    """)
    fun sendEscalationEmail(
        userEmail: String,
        userName: String,
        userMessage: String,
        userPhone: String = "",
        sessionId: String
    ): String {
        logger.info { "AI agent calling sendEscalationEmail for session $sessionId" }

        // Validate required fields
        if (userEmail.isBlank() || userName.isBlank() || userMessage.isBlank()) {
            return "Error: Missing required information. Please ensure you have the user's email, name, and message before calling this tool."
        }

        if (!isValidEmail(userEmail)) {
            return "Error: Invalid email address format. Please ask the user for a valid email address."
        }

        try {
            // Create escalation record
            val escalation = Escalation(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                reason = "User requested human support via AI agent",
                userEmail = userEmail,
                userName = userName,
                userPhone = if (userPhone.isBlank()) null else userPhone,
                userMessage = userMessage,
                conversationMessages = emptyList(), // Will be populated from session if needed
                createdAt = LocalDateTime.now(),
                language = null,
                persona = "customer_service"
            )

            // Save to database for review dashboard
            escalationLogger.logEscalation(
                sessionId = UUID.fromString(escalation.sessionId),
                reason = escalation.reason,
                userEmail = escalation.userEmail,
                userName = escalation.userName ?: "Unknown",
                userPhone = escalation.userPhone,
                userMessage = escalation.userMessage,
                language = escalation.language,
                persona = escalation.persona,
                emailSent = true
            )

            // Send emails
            val emailsSent = sendEmails(escalation)

            return if (emailsSent) {
                "✓ Success! Your request has been sent to our support team. They will contact you at $userEmail ${properties.sendgrid.responseTime}. Reference ID: ${escalation.id}"
            } else {
                "✓ Your request has been recorded (Reference: ${escalation.id}). However, email notifications are currently disabled. Our team will review your request in the dashboard and contact you at $userEmail soon."
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in sendEscalationEmail" }
            return "I apologize, but I encountered an error while submitting your request. Please try contacting our support team directly at ${properties.sendgrid.supportEmail}"
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    private fun sendEmails(escalation: Escalation): Boolean {
        if (sendGrid == null || !properties.sendgrid.enabled) {
            logger.debug { "Email notifications disabled" }
            return false
        }

        var success = true

        // Send confirmation to user
        try {
            sendUserConfirmation(escalation)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send user confirmation" }
            success = false
        }

        // Send notification to support team
        try {
            sendTeamNotification(escalation)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send team notification" }
            success = false
        }

        return success
    }

    private fun sendUserConfirmation(escalation: Escalation) {
        val from = Email(properties.sendgrid.fromEmail, properties.sendgrid.fromName)
        val to = Email(escalation.userEmail)
        val subject = "We've received your request for assistance"

        val htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <h2>Thank you for contacting us!</h2>
                <p>Hi ${escalation.userName},</p>
                <p>We've received your request for assistance and a team member will review it shortly.</p>

                <div style="background: #f5f5f5; padding: 15px; border-left: 4px solid #667eea; margin: 20px 0;">
                    <strong>Your message:</strong><br/>
                    ${escalation.userMessage}
                </div>

                <p><strong>Expected response time:</strong> ${properties.sendgrid.responseTime}</p>
                <p><strong>Reference ID:</strong> ${escalation.id}</p>

                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;"/>
                <p style="font-size: 12px; color: #666;">
                    If you need immediate assistance, please contact us at ${properties.sendgrid.supportEmail}
                </p>
            </body>
            </html>
        """.trimIndent()

        val content = Content("text/html", htmlContent)
        val mail = Mail(from, subject, to, content)

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        val response = sendGrid!!.api(request)

        if (response.statusCode in 200..299) {
            logger.info { "Confirmation email sent to ${escalation.userEmail}" }
        } else {
            logger.error { "Failed to send confirmation email: ${response.statusCode}" }
        }
    }

    private fun sendTeamNotification(escalation: Escalation) {
        val from = Email(properties.sendgrid.fromEmail, properties.sendgrid.fromName)
        val to = Email(properties.sendgrid.supportEmail)
        val subject = "New support request from ${escalation.userName}"

        val htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <h2>New Support Request</h2>

                <div style="background: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0;">
                    <strong>Request Details</strong><br/>
                    <strong>ID:</strong> ${escalation.id}<br/>
                    <strong>Created:</strong> ${escalation.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}<br/>
                    <strong>Session ID:</strong> ${escalation.sessionId}
                </div>

                <h3>Contact Information</h3>
                <p>
                    <strong>Name:</strong> ${escalation.userName}<br/>
                    <strong>Email:</strong> ${escalation.userEmail}<br/>
                    ${if (escalation.userPhone != null) "<strong>Phone:</strong> ${escalation.userPhone}<br/>" else ""}
                </p>

                <h3>Customer's Message</h3>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 4px; white-space: pre-wrap;">
${escalation.userMessage}
                </div>

                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;"/>
                <p>
                    <a href="${properties.sendgrid.dashboardUrl}/wp-admin/admin.php?page=compilot-ai-review"
                       style="background: #667eea; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block;">
                        View in Dashboard
                    </a>
                </p>
            </body>
            </html>
        """.trimIndent()

        val content = Content("text/html", htmlContent)
        val mail = Mail(from, subject, to, content)

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        val response = sendGrid!!.api(request)

        if (response.statusCode in 200..299) {
            logger.info { "Team notification sent for escalation ${escalation.id}" }
        } else {
            logger.error { "Failed to send team notification: ${response.statusCode}" }
        }
    }
}
