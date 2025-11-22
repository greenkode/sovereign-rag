package ai.sovereignrag.core.chat.service.extraction

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for extracting confidence scores from LLM responses
 *
 * Parses [CONFIDENCE: XX%] tags from responses and returns
 * cleaned response text with extracted confidence value.
 */
@Service
class ConfidenceExtractor {

    companion object {
        private val CONFIDENCE_REGEX = """\[CONFIDENCE:\s*(\d+)%?\]""".toRegex()
    }

    /**
     * Extract confidence score from response text
     *
     * @param response Raw response text that may contain [CONFIDENCE: XX%] tag
     * @return Pair of (cleaned response without tag, extracted confidence or null)
     */
    fun extract(response: String): Pair<String, Int?> {
        val match = CONFIDENCE_REGEX.find(response)

        val confidence = match?.groupValues?.get(1)?.toIntOrNull()?.also {
            logger.debug { "Extracted confidence score: $it%" }
        }

        val cleanedResponse = response.replace(CONFIDENCE_REGEX, "").trim()

        return Pair(cleanedResponse, confidence)
    }
}
