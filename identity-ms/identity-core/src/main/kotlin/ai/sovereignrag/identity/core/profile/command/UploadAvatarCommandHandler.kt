package ai.sovereignrag.identity.core.profile.command

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class UploadAvatarCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService,
    private val fileUploadGateway: FileUploadGateway
) : Command.Handler<UploadAvatarCommand, UploadAvatarResult> {

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
        )
    }

    override fun handle(command: UploadAvatarCommand): UploadAvatarResult {
        log.info { "Processing UploadAvatarCommand" }

        if (command.size > MAX_FILE_SIZE) {
            return UploadAvatarResult(
                success = false,
                message = messageService.getMessage("profile.avatar.file_too_large"),
                pictureUrl = null
            )
        }

        if (command.contentType !in ALLOWED_CONTENT_TYPES) {
            return UploadAvatarResult(
                success = false,
                message = messageService.getMessage("profile.avatar.invalid_file"),
                pictureUrl = null
            )
        }

        val user = userService.getCurrentUser()
        val userId = user.id ?: return UploadAvatarResult(
            success = false,
            message = messageService.getMessage("profile.avatar.no_user"),
            pictureUrl = null
        )

        user.pictureUrl?.takeIf { it.startsWith("avatars/") }?.let { oldKey ->
            runCatching {
                fileUploadGateway.deleteFile(oldKey)
                log.info { "Deleted old avatar: $oldKey" }
            }.onFailure { e ->
                log.warn(e) { "Failed to delete old avatar" }
            }
        }

        val uploadResult = fileUploadGateway.uploadUserFile(
            inputStream = command.inputStream,
            fileName = command.fileName,
            contentType = command.contentType,
            size = command.size,
            category = "avatars",
            userId = userId
        )

        user.pictureUrl = uploadResult.key
        userRepository.save(user)

        log.info { "Uploaded avatar for user: $userId, key: ${uploadResult.key}" }

        cacheEvictionService.evictUserCaches(userId.toString())
        cacheEvictionService.evictUserDetailsCaches(userId.toString())

        val presignedUrl = fileUploadGateway.generatePresignedDownloadUrl(uploadResult.key, 60)

        return UploadAvatarResult(
            success = true,
            message = messageService.getMessage("profile.avatar.upload.success"),
            pictureUrl = presignedUrl
        )
    }
}
