package ai.sovereignrag.identity.commons.notification

import java.util.Locale

data class MessagePayload(val recipients: List<MessageRecipient>, val templateName: String,
                          val channel: String, val priority: String, val parameters: Map<String, String>,
    val locale: Locale, val clientIdentifier: String, val recipientType: String)

data class MessageRecipient(val address: String, val name: String? = null)

