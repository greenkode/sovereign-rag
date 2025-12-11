package ai.sovereignrag.core.rag.query

import ai.sovereignrag.knowledgebase.configuration.dto.LlmModelDto
import an.awesome.pipelinr.Command

data class GetAvailableModelsQuery(
    val privacyCompliantOnly: Boolean = false
) : Command<AvailableModelsResult>

data class AvailableModelsResult(
    val models: List<LlmModelDto>,
    val defaultModelId: String
)
