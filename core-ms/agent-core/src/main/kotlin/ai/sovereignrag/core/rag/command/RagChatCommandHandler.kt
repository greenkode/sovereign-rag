package ai.sovereignrag.core.rag.command

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.core.rag.RagChatRequest
import ai.sovereignrag.core.rag.RagService
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class RagChatCommandHandler(
    private val ragService: RagService,
    private val licenseConfiguration: LicenseConfiguration,
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry,
    private val conversationMessageRepository: ConversationMessageRepository
) : Command.Handler<RagChatCommand, RagChatResult> {

    override fun handle(command: RagChatCommand): RagChatResult {
        val conversationId = command.conversationId ?: UUID.randomUUID().toString()

        val knowledgeBaseId = command.conversationId?.let { convId ->
            conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(convId)
                ?.knowledgeBaseId
                ?.toString()
        } ?: command.knowledgeBaseId
        ?: throw IllegalArgumentException("Knowledge base ID required for new conversations")

        val knowledgeBase = knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId)
        val tier = licenseConfiguration.getLicenseInfo().tier

        log.info { "Handling RAG chat for KB $knowledgeBaseId, conversation $conversationId" }

        val ragRequest = RagChatRequest(
            knowledgeBaseId = knowledgeBaseId,
            conversationId = conversationId,
            query = command.message,
            tier = tier,
            modelId = knowledgeBase.llmModelId,
            systemPrompt = knowledgeBase.systemPrompt,
            requiresPrivacy = knowledgeBase.requiresEncryption,
            enableRemiEvaluation = knowledgeBase.enableRemiEvaluation,
            maxResults = knowledgeBase.maxRetrievalResults,
            minScore = knowledgeBase.minSimilarityScore,
            maxHistoryMessages = knowledgeBase.maxHistoryMessages
        )

        val response = ragService.chat(ragRequest)

        return RagChatResult(
            queryId = response.queryId,
            conversationId = response.conversationId,
            answer = response.answer,
            modelUsed = response.modelUsed,
            modelId = response.modelId,
            processingTimeMs = response.processingTimeMs
        )
    }
}
