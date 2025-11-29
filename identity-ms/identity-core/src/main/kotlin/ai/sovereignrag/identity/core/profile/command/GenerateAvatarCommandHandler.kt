package ai.sovereignrag.identity.core.profile.command

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.profile.dto.AvatarStyle
import ai.sovereignrag.identity.core.profile.service.AvatarGenerationProcessService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

@Component
@Transactional
class GenerateAvatarCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService,
    private val fileUploadGateway: FileUploadGateway,
    private val avatarGenerationProcessService: AvatarGenerationProcessService
) : Command.Handler<GenerateAvatarCommand, GenerateAvatarResult> {

    override fun handle(command: GenerateAvatarCommand): GenerateAvatarResult {
        log.info { "Processing GenerateAvatarCommand with style: ${command.style}" }

        val user = userService.getCurrentUser()
        val userId = user.id ?: return GenerateAvatarResult(
            success = false,
            message = messageService.getMessage("profile.avatar.no_user"),
            pictureUrl = null
        )

        val name = user.fullName()
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)

        return when (command.style) {
            AvatarStyle.AI_GENERATED -> handleAiGeneration(command, userId)
            AvatarStyle.INITIALS -> handleStaticAvatar(
                buildInitialsAvatarUrl(encodedName, command.backgroundColor),
                userId
            )
            AvatarStyle.DICEBEAR_AVATAAARS -> handleStaticAvatar(
                buildDiceBearUrl("avataaars", userId.toString()),
                userId
            )
            AvatarStyle.DICEBEAR_BOTTTS -> handleStaticAvatar(
                buildDiceBearUrl("bottts", userId.toString()),
                userId
            )
            AvatarStyle.DICEBEAR_IDENTICON -> handleStaticAvatar(
                buildDiceBearUrl("identicon", userId.toString()),
                userId
            )
            AvatarStyle.DICEBEAR_SHAPES -> handleStaticAvatar(
                buildDiceBearUrl("shapes", userId.toString()),
                userId
            )
        }
    }

    private fun handleAiGeneration(command: GenerateAvatarCommand, userId: java.util.UUID): GenerateAvatarResult {
        val prompt = command.prompt?.takeIf { it.isNotBlank() }
            ?: return GenerateAvatarResult(
                success = false,
                message = messageService.getMessage("profile.avatar.ai.prompt.required"),
                pictureUrl = null
            )

        val result = avatarGenerationProcessService.generateAvatar(userId, prompt, command.processId)

        return GenerateAvatarResult(
            success = result.success,
            message = result.message ?: messageService.getMessage("profile.avatar.generate.success"),
            pictureUrl = result.imageUrl,
            processId = result.processId,
            imageKey = result.imageKey
        )
    }

    private fun handleStaticAvatar(avatarUrl: String, userId: java.util.UUID): GenerateAvatarResult {
        val user = userService.getCurrentUser()
        user.pictureUrl = avatarUrl
        userRepository.save(user)

        log.info { "Generated static avatar for user: $userId, value: $avatarUrl" }

        cacheEvictionService.evictUserCaches(userId.toString())
        cacheEvictionService.evictUserDetailsCaches(userId.toString())

        val responseUrl = avatarUrl.takeIf { it.startsWith("http") }
            ?: fileUploadGateway.generatePresignedDownloadUrl(avatarUrl, 60)

        return GenerateAvatarResult(
            success = true,
            message = messageService.getMessage("profile.avatar.generate.success"),
            pictureUrl = responseUrl
        )
    }

    private fun buildInitialsAvatarUrl(name: String, backgroundColor: String?): String {
        val bgColor = backgroundColor?.removePrefix("#") ?: "6366f1"
        return "https://ui-avatars.com/api/?name=$name&background=$bgColor&color=fff&size=256&bold=true"
    }

    private fun buildDiceBearUrl(style: String, seed: String): String {
        return "https://api.dicebear.com/7.x/$style/svg?seed=$seed&size=256"
    }
}
