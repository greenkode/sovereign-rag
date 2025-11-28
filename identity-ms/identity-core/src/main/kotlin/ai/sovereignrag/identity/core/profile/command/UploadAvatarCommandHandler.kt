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

    override fun handle(command: UploadAvatarCommand): UploadAvatarResult {
        log.info { "Processing UploadAvatarCommand" }

        val user = userService.getCurrentUser()
        val userId = user.id ?: return UploadAvatarResult(
            success = false,
            message = messageService.getMessage("profile.avatar.no_user"),
            pictureUrl = null
        )

        user.pictureUrl?.takeIf { it.contains("/avatars/") }?.let { oldUrl ->
            runCatching {
                val oldKey = oldUrl.substringAfter("/avatars/").let { "avatars/$it" }
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

        user.pictureUrl = uploadResult.url
        userRepository.save(user)

        log.info { "Uploaded avatar for user: $userId, url: ${uploadResult.url}" }

        cacheEvictionService.evictUserCaches(userId.toString())
        cacheEvictionService.evictUserDetailsCaches(userId.toString())

        return UploadAvatarResult(
            success = true,
            message = messageService.getMessage("profile.avatar.upload.success"),
            pictureUrl = uploadResult.url
        )
    }
}
