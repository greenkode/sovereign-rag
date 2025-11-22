package ai.sovereignrag.commons.messaging.dto

import java.io.Serializable

data class MessageRecipient(val address: String, val name: String? = null) : Serializable
