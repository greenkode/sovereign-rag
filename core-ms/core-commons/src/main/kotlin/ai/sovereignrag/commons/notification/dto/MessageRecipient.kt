package ai.sovereignrag.commons.notification.dto

import java.io.Serializable

data class MessageRecipient(val address: String, val name: String? = null) : Serializable
