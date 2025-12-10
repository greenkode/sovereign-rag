package ai.sovereignrag.ingestion.core.remi

import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.entity.EvaluationStatus
import ai.sovereignrag.ingestion.commons.entity.RemiMetrics
import ai.sovereignrag.ingestion.commons.repository.RemiMetricsRepository
import ai.sovereignrag.ingestion.core.metrics.IngestionMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.langchain4j.model.chat.ChatLanguageModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger {}

data class RemiEvaluationRequest(
    val organizationId: UUID,
    val knowledgeBaseId: UUID,
    val queryId: UUID,
    val queryText: String,
    val generatedAnswer: String,
    val retrievedChunks: List<String>,
    val retrievalMetricsId: UUID? = null
)

data class RemiEvaluationResult(
    val answerRelevanceScore: Double?,
    val answerRelevanceReasoning: String?,
    val contextRelevanceScore: Double?,
    val contextRelevanceReasoning: String?,
    val groundednessScore: Double?,
    val groundednessReasoning: String?,
    val hallucinationDetected: Boolean,
    val overallScore: Double?
)

data class ScoreResponse(
    val score: Double,
    val reasoning: String,
    val hallucination_detected: Boolean? = null
)

@Service
class RemiEvaluationService(
    @Qualifier("remiChatModel") private val chatModel: ChatLanguageModel,
    private val remiMetricsRepository: RemiMetricsRepository,
    private val ingestionProperties: IngestionProperties,
    private val objectMapper: ObjectMapper,
    private val ingestionMetrics: IngestionMetrics
) {

    @Async("qualityEvaluationExecutor")
    fun evaluateAsync(request: RemiEvaluationRequest): CompletableFuture<RemiMetrics> {
        return CompletableFuture.supplyAsync {
            evaluate(request)
        }
    }

    fun evaluate(request: RemiEvaluationRequest): RemiMetrics {
        val startTime = System.currentTimeMillis()

        val metrics = RemiMetrics(
            organizationId = request.organizationId,
            knowledgeBaseId = request.knowledgeBaseId,
            queryId = request.queryId,
            queryText = request.queryText
        ).apply {
            generatedAnswer = request.generatedAnswer
            retrievedChunksCount = request.retrievedChunks.size
            retrievalMetricsId = request.retrievalMetricsId
            evaluationStatus = EvaluationStatus.IN_PROGRESS
            evaluationModel = ingestionProperties.remi.modelName
        }

        val saved = remiMetricsRepository.save(metrics)

        return runCatching {
            val combinedContext = request.retrievedChunks.joinToString("\n\n---\n\n")

            val answerRelevance = evaluateAnswerRelevance(request.queryText, request.generatedAnswer)
            saved.answerRelevanceScore = answerRelevance?.score
            saved.answerRelevanceReasoning = answerRelevance?.reasoning

            val contextRelevance = evaluateContextRelevance(request.queryText, combinedContext)
            saved.contextRelevanceScore = contextRelevance?.score
            saved.contextRelevanceReasoning = contextRelevance?.reasoning

            val groundedness = evaluateGroundedness(combinedContext, request.generatedAnswer)
            saved.groundednessScore = groundedness?.score
            saved.groundednessReasoning = groundedness?.reasoning

            saved.evaluatedChunksCount = request.retrievedChunks.size
            saved.evaluationTimeMs = System.currentTimeMillis() - startTime
            saved.markCompleted()

            val result = remiMetricsRepository.save(saved)

            ingestionMetrics.recordRemiEvaluation(
                knowledgeBaseId = request.knowledgeBaseId.toString(),
                answerRelevance = result.answerRelevanceScore,
                contextRelevance = result.contextRelevanceScore,
                groundedness = result.groundednessScore,
                evaluationTimeMs = result.evaluationTimeMs
            )

            log.info {
                "REMI evaluation completed for query ${request.queryId}: " +
                "answer=${result.answerRelevanceScore}, context=${result.contextRelevanceScore}, " +
                "groundedness=${result.groundednessScore}, overall=${result.overallScore}"
            }

            result
        }.getOrElse { e ->
            log.error(e) { "REMI evaluation failed for query ${request.queryId}" }
            saved.markFailed(e.message ?: "Unknown error")
            saved.evaluationTimeMs = System.currentTimeMillis() - startTime
            remiMetricsRepository.save(saved)
        }
    }

    private fun evaluateAnswerRelevance(question: String, answer: String): ScoreResponse? {
        return runCatching {
            val prompt = RemiEvaluationPrompts.buildAnswerRelevancePrompt(question, answer)
            val response = chatModel.generate(prompt)
            parseScoreResponse(response)
        }.onFailure { e ->
            log.warn(e) { "Failed to evaluate answer relevance" }
        }.getOrNull()
    }

    private fun evaluateContextRelevance(question: String, context: String): ScoreResponse? {
        return runCatching {
            val truncatedContext = context.take(8000)
            val prompt = RemiEvaluationPrompts.buildContextRelevancePrompt(question, truncatedContext)
            val response = chatModel.generate(prompt)
            parseScoreResponse(response)
        }.onFailure { e ->
            log.warn(e) { "Failed to evaluate context relevance" }
        }.getOrNull()
    }

    private fun evaluateGroundedness(context: String, answer: String): ScoreResponse? {
        return runCatching {
            val truncatedContext = context.take(8000)
            val prompt = RemiEvaluationPrompts.buildGroundednessPrompt(truncatedContext, answer)
            val response = chatModel.generate(prompt)
            parseScoreResponse(response)
        }.onFailure { e ->
            log.warn(e) { "Failed to evaluate groundedness" }
        }.getOrNull()
    }

    private fun parseScoreResponse(response: String): ScoreResponse {
        val cleanedResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return runCatching {
            objectMapper.readValue<ScoreResponse>(cleanedResponse)
        }.getOrElse {
            val scoreRegex = """"score"\s*:\s*(\d+\.?\d*)""".toRegex()
            val reasoningRegex = """"reasoning"\s*:\s*"([^"]+)"""".toRegex()
            val hallucinationRegex = """"hallucination_detected"\s*:\s*(true|false)""".toRegex()

            val score = scoreRegex.find(cleanedResponse)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.5
            val reasoning = reasoningRegex.find(cleanedResponse)?.groupValues?.get(1) ?: "Unable to parse reasoning"
            val hallucination = hallucinationRegex.find(cleanedResponse)?.groupValues?.get(1)?.toBoolean()

            ScoreResponse(
                score = score.coerceIn(0.0, 1.0),
                reasoning = reasoning,
                hallucination_detected = hallucination
            )
        }
    }

    fun findByQueryId(queryId: UUID): RemiMetrics? {
        return remiMetricsRepository.findByQueryId(queryId)
    }
}
