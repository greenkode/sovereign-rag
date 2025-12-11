package ai.sovereignrag.core.rag.memory.integration

import ai.sovereignrag.core.config.AbstractIntegrationTest
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import ai.sovereignrag.core.rag.memory.JpaChatMemoryStore
import ai.sovereignrag.core.rag.memory.MessageType
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(JpaChatMemoryStore::class)
class JpaChatMemoryStoreIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var conversationMessageRepository: ConversationMessageRepository

    @Autowired
    private lateinit var chatMemoryStore: JpaChatMemoryStore

    private lateinit var testOrganizationId: UUID
    private lateinit var testKnowledgeBaseId: UUID
    private lateinit var testConversationId: String

    @BeforeEach
    fun setup() {
        testOrganizationId = UUID.randomUUID()
        testKnowledgeBaseId = UUID.randomUUID()
        testConversationId = UUID.randomUUID().toString()
        chatMemoryStore.setContext(testOrganizationId, testKnowledgeBaseId)
    }

    @Test
    fun `should store and retrieve messages`() {
        val messages = listOf(
            SystemMessage.from("You are a helpful assistant"),
            UserMessage.from("Hello"),
            AiMessage.from("Hi there! How can I help you?")
        )

        chatMemoryStore.updateMessages(testConversationId, messages)

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertEquals(3, retrieved.size)
        assertTrue(retrieved[0] is SystemMessage)
        assertTrue(retrieved[1] is UserMessage)
        assertTrue(retrieved[2] is AiMessage)
        assertEquals("You are a helpful assistant", (retrieved[0] as SystemMessage).text())
        assertEquals("Hello", (retrieved[1] as UserMessage).singleText())
        assertEquals("Hi there! How can I help you?", (retrieved[2] as AiMessage).text())
    }

    @Test
    fun `should maintain message order by sequence number`() {
        val messages = listOf(
            UserMessage.from("First message"),
            AiMessage.from("First response"),
            UserMessage.from("Second message"),
            AiMessage.from("Second response"),
            UserMessage.from("Third message")
        )

        chatMemoryStore.updateMessages(testConversationId, messages)

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertEquals(5, retrieved.size)
        assertEquals("First message", (retrieved[0] as UserMessage).singleText())
        assertEquals("First response", (retrieved[1] as AiMessage).text())
        assertEquals("Second message", (retrieved[2] as UserMessage).singleText())
        assertEquals("Second response", (retrieved[3] as AiMessage).text())
        assertEquals("Third message", (retrieved[4] as UserMessage).singleText())
    }

    @Test
    fun `should update messages by replacing existing`() {
        val initialMessages = listOf(
            UserMessage.from("Old message"),
            AiMessage.from("Old response")
        )
        chatMemoryStore.updateMessages(testConversationId, initialMessages)

        val updatedMessages = listOf(
            UserMessage.from("New message"),
            AiMessage.from("New response"),
            UserMessage.from("Follow up")
        )
        chatMemoryStore.updateMessages(testConversationId, updatedMessages)

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertEquals(3, retrieved.size)
        assertEquals("New message", (retrieved[0] as UserMessage).singleText())
        assertEquals("New response", (retrieved[1] as AiMessage).text())
        assertEquals("Follow up", (retrieved[2] as UserMessage).singleText())
    }

    @Test
    fun `should delete messages for conversation`() {
        val messages = listOf(
            UserMessage.from("Test message"),
            AiMessage.from("Test response")
        )
        chatMemoryStore.updateMessages(testConversationId, messages)

        chatMemoryStore.deleteMessages(testConversationId)

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun `should delete all messages for knowledge base`() {
        val conversationId1 = UUID.randomUUID().toString()
        val conversationId2 = UUID.randomUUID().toString()

        chatMemoryStore.updateMessages(conversationId1, listOf(UserMessage.from("Message 1")))
        chatMemoryStore.updateMessages(conversationId2, listOf(UserMessage.from("Message 2")))

        chatMemoryStore.deleteAllForKnowledgeBase(testKnowledgeBaseId)

        assertTrue(chatMemoryStore.getMessages(conversationId1).isEmpty())
        assertTrue(chatMemoryStore.getMessages(conversationId2).isEmpty())
    }

    @Test
    fun `should delete all messages for organization`() {
        val conversationId1 = UUID.randomUUID().toString()
        val conversationId2 = UUID.randomUUID().toString()

        chatMemoryStore.updateMessages(conversationId1, listOf(UserMessage.from("Org message 1")))
        chatMemoryStore.updateMessages(conversationId2, listOf(UserMessage.from("Org message 2")))

        chatMemoryStore.deleteAllForOrganization(testOrganizationId)

        assertTrue(chatMemoryStore.getMessages(conversationId1).isEmpty())
        assertTrue(chatMemoryStore.getMessages(conversationId2).isEmpty())
    }

    @Test
    fun `should store message with correct organization and knowledge base context`() {
        val messages = listOf(UserMessage.from("Context test"))
        chatMemoryStore.updateMessages(testConversationId, messages)

        val entities = conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(testConversationId)

        assertEquals(1, entities.size)
        assertEquals(testOrganizationId, entities[0].organizationId)
        assertEquals(testKnowledgeBaseId, entities[0].knowledgeBaseId)
    }

    @Test
    fun `should handle empty message list`() {
        chatMemoryStore.updateMessages(testConversationId, emptyList())

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun `should return empty list for non-existent conversation`() {
        val retrieved = chatMemoryStore.getMessages("non-existent-conversation-id")

        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun `should isolate messages between different conversations`() {
        val conversationId1 = UUID.randomUUID().toString()
        val conversationId2 = UUID.randomUUID().toString()

        chatMemoryStore.updateMessages(conversationId1, listOf(UserMessage.from("Conv1 message")))
        chatMemoryStore.updateMessages(conversationId2, listOf(UserMessage.from("Conv2 message")))

        val conv1Messages = chatMemoryStore.getMessages(conversationId1)
        val conv2Messages = chatMemoryStore.getMessages(conversationId2)

        assertEquals(1, conv1Messages.size)
        assertEquals("Conv1 message", (conv1Messages[0] as UserMessage).singleText())
        assertEquals(1, conv2Messages.size)
        assertEquals("Conv2 message", (conv2Messages[0] as UserMessage).singleText())
    }

    @Test
    fun `should persist correct message types in database`() {
        val messages = listOf(
            SystemMessage.from("System"),
            UserMessage.from("User"),
            AiMessage.from("AI")
        )

        chatMemoryStore.updateMessages(testConversationId, messages)

        val entities = conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(testConversationId)

        assertEquals(3, entities.size)
        assertEquals(MessageType.SYSTEM, entities[0].messageType)
        assertEquals(MessageType.USER, entities[1].messageType)
        assertEquals(MessageType.AI, entities[2].messageType)
    }

    @Test
    fun `should handle long message content`() {
        val longContent = "A".repeat(10000)
        val messages = listOf(UserMessage.from(longContent))

        chatMemoryStore.updateMessages(testConversationId, messages)

        val retrieved = chatMemoryStore.getMessages(testConversationId)

        assertEquals(1, retrieved.size)
        assertEquals(longContent, (retrieved[0] as UserMessage).singleText())
    }
}
