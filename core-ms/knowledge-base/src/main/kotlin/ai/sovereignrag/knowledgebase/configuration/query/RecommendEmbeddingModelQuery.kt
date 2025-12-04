package ai.sovereignrag.knowledgebase.configuration.query

import ai.sovereignrag.knowledgebase.configuration.query.result.RecommendEmbeddingModelResult
import an.awesome.pipelinr.Command

data class RecommendEmbeddingModelQuery(
    val languageCodes: Set<String>
) : Command<RecommendEmbeddingModelResult>
