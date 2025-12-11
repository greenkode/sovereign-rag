package ai.sovereignrag.core.rag.memory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationMessageRepository : JpaRepository<ConversationMessage, UUID> {

    fun findByConversationIdOrderBySequenceNumberAsc(conversationId: String): List<ConversationMessage>

    fun findByConversationIdOrderByCreatedAtAsc(conversationId: String): List<ConversationMessage>

    fun findFirstByConversationIdOrderBySequenceNumberAsc(conversationId: String): ConversationMessage?

    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM ConversationMessage m WHERE m.conversationId = :conversationId")
    fun findMaxSequenceNumber(conversationId: String): Int

    @Modifying
    @Query("DELETE FROM ConversationMessage m WHERE m.conversationId = :conversationId")
    fun deleteByConversationId(conversationId: String)

    fun countByConversationId(conversationId: String): Long

    @Query("SELECT DISTINCT m.conversationId FROM ConversationMessage m WHERE m.organizationId = :organizationId")
    fun findConversationIdsByOrganization(organizationId: UUID): List<String>

    @Query("SELECT DISTINCT m.conversationId FROM ConversationMessage m WHERE m.knowledgeBaseId = :knowledgeBaseId")
    fun findConversationIdsByKnowledgeBase(knowledgeBaseId: UUID): List<String>

    @Modifying
    @Query("DELETE FROM ConversationMessage m WHERE m.knowledgeBaseId = :knowledgeBaseId")
    fun deleteByKnowledgeBaseId(knowledgeBaseId: UUID)

    @Modifying
    @Query("DELETE FROM ConversationMessage m WHERE m.organizationId = :organizationId")
    fun deleteByOrganizationId(organizationId: UUID)
}
