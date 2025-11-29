package ai.sovereignrag.identity.core.profile.service

import dev.langchain4j.data.image.Image
import dev.langchain4j.model.image.ImageModel
import dev.langchain4j.model.openai.OpenAiImageModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

private val log = KotlinLogging.logger {}

@Service
class AvatarImageGenerationService(
    @Value("\${sovereignrag.openai.api-key:}")
    private val openAiApiKey: String,
    @Value("\${sovereignrag.openai.image-model:dall-e-3}")
    private val modelName: String,
    @Value("\${sovereignrag.openai.image-size:1024x1024}")
    private val imageSize: String
) {
    private val imageModel: ImageModel? by lazy {
        openAiApiKey.takeIf { it.isNotBlank() }?.let {
            OpenAiImageModel.builder()
                .apiKey(it)
                .modelName(modelName)
                .size(imageSize)
                .responseFormat("b64_json")
                .build()
        }
    }

    fun generateAvatar(prompt: String): AvatarGenerationResult {
        log.info { "Generating AI avatar with prompt: $prompt" }

        val model = imageModel ?: return AvatarGenerationResult(
            success = false,
            errorMessage = "OpenAI API key not configured",
            imageBytes = null
        )

        val enhancedPrompt = buildAvatarPrompt(prompt)

        return runCatching {
            val response = model.generate(enhancedPrompt)
            val image = response.content()

            val imageBytes = image.base64Data()?.let {
                Base64.getDecoder().decode(it)
            } ?: image.url()?.let { url ->
                java.net.URI(url.toString()).toURL().readBytes()
            }

            imageBytes?.let {
                log.info { "Successfully generated AI avatar, size: ${it.size} bytes" }
                AvatarGenerationResult(
                    success = true,
                    errorMessage = null,
                    imageBytes = it
                )
            } ?: AvatarGenerationResult(
                success = false,
                errorMessage = "Failed to extract image data from response",
                imageBytes = null
            )
        }.getOrElse { e ->
            log.error(e) { "Failed to generate AI avatar" }
            AvatarGenerationResult(
                success = false,
                errorMessage = e.message ?: "Unknown error during image generation",
                imageBytes = null
            )
        }
    }

    private fun buildAvatarPrompt(userPrompt: String): String {
        return """
            Create a professional avatar portrait image based on this description: $userPrompt

            CRITICAL CONTENT REQUIREMENTS:
            - The image MUST be safe for work (SFW) and appropriate for all audiences
            - NO nudity, partial nudity, or suggestive content
            - NO horror, gore, violence, or disturbing imagery
            - NO offensive, controversial, or inappropriate content
            - The image should be suitable for a corporate/professional environment

            Style requirements:
            - Clean, modern digital art style suitable for a professional profile picture
            - Centered composition with the subject filling most of the frame
            - Soft, professional lighting
            - Neutral or gradient background (business-appropriate colors)
            - High quality, detailed rendering
            - Professional, friendly, and approachable appearance
            - Appropriate for business/corporate use on professional platforms
        """.trimIndent()
    }

    fun isConfigured(): Boolean = openAiApiKey.isNotBlank()
}

data class AvatarGenerationResult(
    val success: Boolean,
    val errorMessage: String?,
    val imageBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AvatarGenerationResult
        if (success != other.success) return false
        if (errorMessage != other.errorMessage) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}
