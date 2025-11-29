package ai.sovereignrag.identity.core.profile.service

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.UUID

private val log = KotlinLogging.logger {}

data class AvatarSession(
    val processId: UUID,
    val prompts: List<String>,
    val currentImageKey: String?,
    val currentImageUrl: String?
)

data class AvatarGenerationProcessResult(
    val success: Boolean,
    val message: String?,
    val imageKey: String?,
    val imageUrl: String?,
    val processId: UUID?
)

@Service
class AvatarGenerationProcessService(
    private val processGateway: ProcessGateway,
    private val avatarImageGenerationService: AvatarImageGenerationService,
    private val promptRefinementService: PromptRefinementService,
    private val fileUploadGateway: FileUploadGateway
) {

    fun getOrCreateSession(userId: UUID): AvatarSession {
        val existingProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            ProcessType.AVATAR_GENERATION,
            userId
        )

        existingProcess?.let { process ->
            val prompts = process.requests
                .filter { it.type == ProcessRequestType.AVATAR_PROMPT || it.type == ProcessRequestType.AVATAR_REFINEMENT }
                .sortedBy { it.id }
                .mapNotNull { request ->
                    request.data[ProcessRequestDataName.AVATAR_PROMPT]
                }

            val currentImageKey = process.requests
                .sortedByDescending { it.id }
                .firstNotNullOfOrNull { request ->
                    request.data[ProcessRequestDataName.AVATAR_IMAGE_KEY]
                }

            val currentImageUrl = currentImageKey?.let {
                fileUploadGateway.generatePresignedDownloadUrl(it, 60)
            }

            return AvatarSession(
                processId = process.publicId,
                prompts = prompts,
                currentImageKey = currentImageKey,
                currentImageUrl = currentImageUrl
            )
        }

        val newProcessId = UUID.randomUUID()
        processGateway.createProcess(
            CreateNewProcessPayload(
                publicId = newProcessId,
                type = ProcessType.AVATAR_GENERATION,
                description = "Avatar Generation Session",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.PENDING,
                channel = Channel.BUSINESS_WEB,
                userId = userId,
                stakeholders = mapOf(ProcessStakeholderType.FOR_USER to userId.toString()),
                data = emptyMap()
            )
        )

        return AvatarSession(
            processId = newProcessId,
            prompts = emptyList(),
            currentImageKey = null,
            currentImageUrl = null
        )
    }

    fun generateAvatar(userId: UUID, prompt: String, processId: UUID?): AvatarGenerationProcessResult {
        val session = processId?.let { getSessionByProcessId(it, userId) }
            ?: getOrCreateSession(userId)

        val refinedPrompt = session.prompts.takeIf { it.isNotEmpty() }?.let {
            promptRefinementService.refinePrompt(it, prompt)
        } ?: prompt

        log.info { "Generating avatar with refined prompt: $refinedPrompt" }

        if (!avatarImageGenerationService.isConfigured()) {
            return AvatarGenerationProcessResult(
                success = false,
                message = "AI avatar generation is not configured",
                imageKey = null,
                imageUrl = null,
                processId = session.processId
            )
        }

        val generationResult = avatarImageGenerationService.generateAvatar(refinedPrompt)
        if (!generationResult.success || generationResult.imageBytes == null) {
            return AvatarGenerationProcessResult(
                success = false,
                message = generationResult.errorMessage ?: "Failed to generate avatar",
                imageKey = null,
                imageUrl = null,
                processId = session.processId
            )
        }

        val fileName = "${UUID.randomUUID()}.png"
        val uploadResult = fileUploadGateway.uploadUserFile(
            inputStream = ByteArrayInputStream(generationResult.imageBytes),
            fileName = fileName,
            contentType = "image/png",
            size = generationResult.imageBytes.size.toLong(),
            category = "avatars",
            userId = userId
        )

        val requestType = session.prompts.isEmpty()
            .let { if (it) ProcessRequestType.AVATAR_PROMPT else ProcessRequestType.AVATAR_REFINEMENT }

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                processPublicId = session.processId,
                userId = userId,
                requestType = requestType,
                state = ProcessState.COMPLETE,
                channel = Channel.BUSINESS_WEB,
                eventType = ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED,
                stakeholders = emptyMap(),
                data = mapOf(
                    ProcessRequestDataName.AVATAR_PROMPT to prompt,
                    ProcessRequestDataName.AVATAR_REFINED_PROMPT to refinedPrompt,
                    ProcessRequestDataName.AVATAR_IMAGE_KEY to uploadResult.key
                )
            )
        )

        val presignedUrl = fileUploadGateway.generatePresignedDownloadUrl(uploadResult.key, 60)

        return AvatarGenerationProcessResult(
            success = true,
            message = null,
            imageKey = uploadResult.key,
            imageUrl = presignedUrl,
            processId = session.processId
        )
    }

    fun completeSession(userId: UUID, processId: UUID, selectedImageKey: String): String {
        processGateway.makeRequest(
            MakeProcessRequestPayload(
                processPublicId = processId,
                userId = userId,
                requestType = ProcessRequestType.COMPLETE_PROCESS,
                state = ProcessState.COMPLETE,
                channel = Channel.BUSINESS_WEB,
                eventType = ProcessEvent.PROCESS_COMPLETED,
                stakeholders = emptyMap(),
                data = mapOf(ProcessRequestDataName.AVATAR_IMAGE_KEY to selectedImageKey)
            )
        )

        return selectedImageKey
    }

    fun cancelSession(userId: UUID, processId: UUID) {
        processGateway.failProcess(processId)
    }

    private fun getSessionByProcessId(processId: UUID, userId: UUID): AvatarSession? {
        val process = processGateway.findPendingProcessByPublicId(processId) ?: return null

        val prompts = process.requests
            .filter { it.type == ProcessRequestType.AVATAR_PROMPT || it.type == ProcessRequestType.AVATAR_REFINEMENT }
            .sortedBy { it.id }
            .mapNotNull { request ->
                request.data[ProcessRequestDataName.AVATAR_PROMPT]
            }

        val currentImageKey = process.requests
            .sortedByDescending { it.id }
            .firstNotNullOfOrNull { request ->
                request.data[ProcessRequestDataName.AVATAR_IMAGE_KEY]
            }

        val currentImageUrl = currentImageKey?.let {
            fileUploadGateway.generatePresignedDownloadUrl(it, 60)
        }

        return AvatarSession(
            processId = process.publicId,
            prompts = prompts,
            currentImageKey = currentImageKey,
            currentImageUrl = currentImageUrl
        )
    }
}
