package ai.sovereignrag.core.rag.memory

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class JpaChatMemoryStore(
    private val conversationMessageRepository: ConversationMessageRepository
) : ChatMemoryStore {

    private var currentOrganizationId: UUID? = null
    private var currentKnowledgeBaseId: UUID? = null

    fun setContext(organizationId: UUID?, knowledgeBaseId: UUID?) {
        this.currentOrganizationId = organizationId
        this.currentKnowledgeBaseId = knowledgeBaseId
    }

    override fun getMessages(memoryId: Any): List<ChatMessage> {
        val conversationId = memoryId.toString()
        log.debug { "Loading messages for conversation: $conversationId" }

        val entities = conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId)
        return entities.map { it.toChatMessage() }
    }

    @Transactional
    override fun updateMessages(memoryId: Any, messages: List<ChatMessage>) {
        val conversationId = memoryId.toString()
        log.debug { "Updating ${messages.size} messages for conversation: $conversationId" }

        conversationMessageRepository.deleteByConversationId(conversationId)

        messages.forEachIndexed { index, message ->
            val entity = ConversationMessage(
                conversationId = conversationId,
                organizationId = currentOrganizationId,
                knowledgeBaseId = currentKnowledgeBaseId,
                messageType = message.toMessageType(),
                content = message.toContent(),
                createdAt = Instant.now(),
                sequenceNumber = index
            )
            conversationMessageRepository.save(entity)
        }
    }

    @Transactional
    override fun deleteMessages(memoryId: Any) {
        val conversationId = memoryId.toString()
        log.debug { "Deleting messages for conversation: $conversationId" }
        conversationMessageRepository.deleteByConversationId(conversationId)
    }

    @Transactional
    fun deleteAllForKnowledgeBase(knowledgeBaseId: UUID) {
        log.info { "Deleting all messages for knowledge base: $knowledgeBaseId" }
        conversationMessageRepository.deleteByKnowledgeBaseId(knowledgeBaseId)
    }

    @Transactional
    fun deleteAllForOrganization(organizationId: UUID) {
        log.info { "Deleting all messages for organization: $organizationId" }
        conversationMessageRepository.deleteByOrganizationId(organizationId)
    }

    private fun ConversationMessage.toChatMessage(): ChatMessage {
        return when (messageType) {
            MessageType.SYSTEM -> SystemMessage.from(content)
            MessageType.USER -> UserMessage.from(content)
            MessageType.AI -> AiMessage.from(content)
        }
    }

    private fun ChatMessage.toMessageType(): MessageType {
        return when (this) {
            is SystemMessage -> MessageType.SYSTEM
            is UserMessage -> MessageType.USER
            is AiMessage -> MessageType.AI
            else -> MessageType.USER
        }
    }

    private fun ChatMessage.toContent(): String {
        return when (this) {
            is SystemMessage -> text()
            is UserMessage -> singleText()
            is AiMessage -> text()
            else -> toString()
        }
    }
}
