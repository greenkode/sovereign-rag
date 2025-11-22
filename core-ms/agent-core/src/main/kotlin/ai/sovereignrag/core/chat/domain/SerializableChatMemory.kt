package ai.sovereignrag.core.chat.domain

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import java.io.Serializable

/**
 * Serializable wrapper for LangChain4j ChatMemory
 * Stores messages as serializable data and reconstructs ChatMemory on demand
 */
class SerializableChatMemory(
    private val maxMessages: Int = 10
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    // Store messages as simple serializable data
    private val storedMessages: MutableList<StoredMessage> = mutableListOf()

    // Transient ChatMemory instance (recreated after deserialization)
    @Transient
    private var chatMemory: ChatMemory? = null

    /**
     * Get the underlying ChatMemory instance
     */
    fun getChatMemory(): ChatMemory {
        // Ensure chatMemory is initialized (important after deserialization)
        if (chatMemory == null) {
            chatMemory = MessageWindowChatMemory.withMaxMessages(maxMessages)
            // Restore messages from stored data
            storedMessages.forEach { stored ->
                chatMemory!!.add(stored.toChatMessage())
            }
        }
        return chatMemory!!
    }

    /**
     * Add a message to the chat memory
     */
    fun add(message: ChatMessage) {
        getChatMemory().add(message)

        // Store the message for serialization
        val stored = StoredMessage.from(message)
        storedMessages.add(stored)

        // Maintain max messages limit
        if (storedMessages.size > maxMessages) {
            storedMessages.removeAt(0)
        }
    }

    /**
     * Get all messages
     */
    fun messages(): List<ChatMessage> {
        return getChatMemory().messages()
    }

    /**
     * Clear all messages
     */
    fun clear() {
        getChatMemory().clear()
        storedMessages.clear()
    }

    /**
     * Recreate ChatMemory after deserialization
     */
    private fun readResolve(): Any {
        val memory = MessageWindowChatMemory.withMaxMessages(maxMessages)
        // Restore messages from stored data
        storedMessages.forEach { stored ->
            memory.add(stored.toChatMessage())
        }
        chatMemory = memory
        return this
    }

    /**
     * Simple serializable representation of a chat message
     */
    private data class StoredMessage(
        val type: MessageType,
        val text: String
    ) : Serializable {

        companion object {
            private const val serialVersionUID = 1L

            fun from(message: ChatMessage): StoredMessage {
                return when (message) {
                    is UserMessage -> StoredMessage(MessageType.USER, message.singleText())
                    is AiMessage -> StoredMessage(MessageType.AI, message.text())
                    is SystemMessage -> StoredMessage(MessageType.SYSTEM, message.text())
                    else -> StoredMessage(MessageType.SYSTEM, message.text())
                }
            }
        }

        fun toChatMessage(): ChatMessage {
            return when (type) {
                MessageType.USER -> UserMessage(text)
                MessageType.AI -> AiMessage(text)
                MessageType.SYSTEM -> SystemMessage(text)
            }
        }
    }

    private enum class MessageType : Serializable {
        USER, AI, SYSTEM
    }
}
