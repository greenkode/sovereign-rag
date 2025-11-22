package ai.sovereignrag.core.chat.domain

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.memory.ChatMemory
import java.io.Serializable
import java.time.LocalDateTime

data class ChatSession(
    val sessionId: String,
    val persona: String = "customer_service",
    var language: String? = null,
    val serializableChatMemory: SerializableChatMemory = SerializableChatMemory(maxMessages = 10),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var lastAccessedAt: LocalDateTime = LocalDateTime.now()
) : Serializable {
    companion object {
        private const val serialVersionUID = 2L // Increment version due to schema change
    }

    /**
     * Get the ChatMemory interface for compatibility with existing code
     */
    val chatMemory: ChatMemory
        get() = object : ChatMemory {
            override fun id(): Any = sessionId
            override fun add(message: ChatMessage) = serializableChatMemory.add(message)
            override fun messages(): List<ChatMessage> = serializableChatMemory.messages()
            override fun clear() = serializableChatMemory.clear()
        }
}
