package ai.sovereignrag.core.service

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import ai.sovereignrag.domain.SearchResult
import org.springframework.stereotype.Service
import java.net.URL
import java.nio.LongBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.math.exp

private val logger = KotlinLogging.logger {}

/**
 * Cross-Encoder Re-ranking Service using ONNX Runtime
 *
 * Uses a cross-encoder model to re-rank search results for improved accuracy.
 * The cross-encoder jointly encodes query + document pairs to produce relevance scores.
 *
 * Model: cross-encoder/ms-marco-MiniLM-L-6-v2 (ONNX format)
 * Framework: ONNX Runtime with HuggingFace Tokenizers
 */
@Service
class RerankerService {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: HuggingFaceTokenizer? = null
    private var isAvailable = false

    companion object {
        private const val MODEL_NAME = "cross-encoder/ms-marco-MiniLM-L-6-v2"
        private const val ONNX_MODEL_URL = "https://huggingface.co/cross-encoder/ms-marco-MiniLM-L-6-v2/resolve/main/onnx/model.onnx"
        private const val TOKENIZER_URL = "https://huggingface.co/cross-encoder/ms-marco-MiniLM-L-6-v2/raw/main/tokenizer.json"
        private const val MAX_LENGTH = 512
    }

    @PostConstruct
    fun initialize() {
        try {
            logger.info { "Initializing cross-encoder model for re-ranking..." }
            logger.info { "Model: $MODEL_NAME (ONNX format)" }

            // Create cache directory
            val cacheDir = Path.of(System.getProperty("user.home"), ".cache", "sovereign-rag", "models")
            Files.createDirectories(cacheDir)

            // Download model file if not exists
            val modelPath = cacheDir.resolve("cross-encoder-ms-marco-minilm-l6-v2.onnx")
            if (!Files.exists(modelPath)) {
                logger.info { "Downloading ONNX model from HuggingFace..." }
                downloadFile(ONNX_MODEL_URL, modelPath)
                logger.info { "Model downloaded successfully" }
            } else {
                logger.info { "Using cached model from: $modelPath" }
            }

            // Download tokenizer if not exists
            val tokenizerPath = cacheDir.resolve("tokenizer.json")
            if (!Files.exists(tokenizerPath)) {
                logger.info { "Downloading tokenizer from HuggingFace..." }
                downloadFile(TOKENIZER_URL, tokenizerPath)
                logger.info { "Tokenizer downloaded successfully" }
            } else {
                logger.info { "Using cached tokenizer from: $tokenizerPath" }
            }

            // Initialize ONNX Runtime environment
            ortEnv = OrtEnvironment.getEnvironment()

            // Load model
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnv!!.createSession(modelPath.toString(), sessionOptions)
            logger.info { "ONNX model loaded successfully" }

            // Load tokenizer
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, mapOf(
                "padding" to "max_length",
                "maxLength" to MAX_LENGTH.toString(),
                "truncation" to "true"
            ))
            logger.info { "Tokenizer loaded successfully" }

            isAvailable = true
            logger.info { "Cross-encoder re-ranking initialized successfully" }

        } catch (e: Exception) {
            logger.warn(e) { "Failed to load cross-encoder model. Re-ranking will be disabled. Error: ${e.message}" }
            logger.warn { "Cross-encoder requires internet connection for first-time model download" }
            isAvailable = false
            cleanup()
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            session?.close()
            ortEnv?.close()
            tokenizer?.close()
            logger.info { "Cross-encoder model resources released" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing cross-encoder model resources" }
        }
    }

    /**
     * Re-rank search results using cross-encoder model (with parallel processing)
     */
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (!isAvailable || session == null || tokenizer == null) {
            logger.debug { "Cross-encoder not available, returning original results" }
            return results
        }

        if (results.isEmpty()) {
            return results
        }

        try {
            logger.debug { "Re-ranking ${results.size} results with cross-encoder (parallel)" }

            // Process all results in parallel using coroutines
            val rerankedResults = coroutineScope {
                results.map { result ->
                    async(Dispatchers.Default) {
                        val score = scoreQueryDocumentPair(query, result.fact)
                        result.copy(confidence = score)
                    }
                }.awaitAll()
            }

            // Sort by new cross-encoder scores (descending)
            val sorted = rerankedResults.sortedByDescending { it.confidence }

            logger.debug {
                "Re-ranking complete. Top score before: ${String.format("%.4f", results.first().confidence)} " +
                "after: ${String.format("%.4f", sorted.first().confidence)}"
            }

            return sorted
        } catch (e: Exception) {
            logger.error(e) { "Error during re-ranking, returning original results" }
            return results
        }
    }

    /**
     * Score a single query-document pair using the cross-encoder
     */
    private fun scoreQueryDocumentPair(query: String, document: String): Double {
        val currentSession = session ?: return 0.0
        val currentTokenizer = tokenizer ?: return 0.0
        val currentEnv = ortEnv ?: return 0.0

        return try {
            // Tokenize the pair: [CLS] query [SEP] document [SEP]
            val encoding = currentTokenizer.encode(query, document)

            // Prepare input tensors
            val inputIds = encoding.ids
            val attentionMask = encoding.attentionMask
            val tokenTypeIds = encoding.typeIds

            // Convert to ONNX tensors
            val inputIdsTensor = OnnxTensor.createTensor(
                currentEnv,
                LongBuffer.wrap(inputIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, inputIds.size.toLong())
            )

            val attentionMaskTensor = OnnxTensor.createTensor(
                currentEnv,
                LongBuffer.wrap(attentionMask.map { it.toLong() }.toLongArray()),
                longArrayOf(1, attentionMask.size.toLong())
            )

            val tokenTypeIdsTensor = OnnxTensor.createTensor(
                currentEnv,
                LongBuffer.wrap(tokenTypeIds.map { it.toLong() }.toLongArray()),
                longArrayOf(1, tokenTypeIds.size.toLong())
            )

            // Run inference
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            val output = currentSession.run(inputs)

            // Get logits (output is typically [batch_size, 1] for cross-encoders)
            val logits = output.get(0).value as Array<FloatArray>
            val rawScore = logits[0][0].toDouble()

            // Clean up tensors
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
            output.close()

            // Apply sigmoid normalization to convert to probability
            normalizeCrossEncoderScore(rawScore)

        } catch (e: Exception) {
            logger.warn(e) { "Error scoring pair, returning 0.0" }
            0.0
        }
    }

    /**
     * Normalize cross-encoder score using sigmoid function
     * Converts raw logits to probabilities in range [0, 1]
     */
    private fun normalizeCrossEncoderScore(score: Double): Double {
        return 1.0 / (1.0 + exp(-score))
    }

    /**
     * Download file from URL
     */
    private fun downloadFile(urlString: String, destination: Path) {
        val url = URL(urlString)
        url.openStream().use { input ->
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Check if the re-ranker is available and ready to use
     */
    fun isAvailable(): Boolean = isAvailable
}
