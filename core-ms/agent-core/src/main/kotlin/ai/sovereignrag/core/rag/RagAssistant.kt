package ai.sovereignrag.core.rag

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage

interface RagAssistant {
    fun chat(@MemoryId conversationId: String, @UserMessage message: String): String
}
