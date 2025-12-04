package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.GetEmbeddingModelsResult
import an.awesome.pipelinr.Command

data class GetEmbeddingModelsQuery(
    val languageCodes: Set<String>? = null
) : Command<GetEmbeddingModelsResult>
