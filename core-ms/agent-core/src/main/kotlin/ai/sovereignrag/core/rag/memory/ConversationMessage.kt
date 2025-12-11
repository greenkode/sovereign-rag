package ai.sovereignrag.core.rag.memory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "conversation_message",
    indexes = [
        Index(name = "idx_conversation_id", columnList = "conversationId"),
        Index(name = "idx_conversation_created", columnList = "conversationId, createdAt")
    ]
)
class ConversationMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    var conversationId: String = "",

    var organizationId: UUID? = null,

    var knowledgeBaseId: UUID? = null,

    @Enumerated(EnumType.STRING)
    var messageType: MessageType = MessageType.USER,

    @Lob
    @Column(columnDefinition = "TEXT")
    var content: String = "",

    var createdAt: Instant = Instant.now(),

    var sequenceNumber: Int = 0
)

enum class MessageType {
    SYSTEM,
    USER,
    AI
}
