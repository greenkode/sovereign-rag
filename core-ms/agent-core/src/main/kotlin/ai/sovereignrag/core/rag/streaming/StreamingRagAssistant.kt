package ai.sovereignrag.core.rag.streaming

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage

interface StreamingRagAssistant {
    fun chat(@MemoryId conversationId: String, @UserMessage message: String): TokenStream
}
