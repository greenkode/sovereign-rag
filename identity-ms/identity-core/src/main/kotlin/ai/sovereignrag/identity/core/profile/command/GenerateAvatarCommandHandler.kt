package ai.sovereignrag.identity.core.profile.command

import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.profile.dto.AvatarStyle
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
    private val messageService: MessageService
) : Command.Handler<GenerateAvatarCommand, GenerateAvatarResult> {

    override fun handle(command: GenerateAvatarCommand): GenerateAvatarResult {
        log.info { "Processing GenerateAvatarCommand with style: ${command.style}" }

        val user = userService.getCurrentUser()
        val name = user.fullName()
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8)

        val avatarUrl = when (command.style) {
            AvatarStyle.INITIALS -> buildInitialsAvatarUrl(encodedName, command.backgroundColor)
            AvatarStyle.DICEBEAR_AVATAAARS -> buildDiceBearUrl("avataaars", user.id.toString())
            AvatarStyle.DICEBEAR_BOTTTS -> buildDiceBearUrl("bottts", user.id.toString())
            AvatarStyle.DICEBEAR_IDENTICON -> buildDiceBearUrl("identicon", user.id.toString())
            AvatarStyle.DICEBEAR_SHAPES -> buildDiceBearUrl("shapes", user.id.toString())
        }

        user.pictureUrl = avatarUrl
        userRepository.save(user)

        log.info { "Generated avatar for user: ${user.id}, style: ${command.style}, url: $avatarUrl" }

        user.id?.let { userId ->
            cacheEvictionService.evictUserCaches(userId.toString())
            cacheEvictionService.evictUserDetailsCaches(userId.toString())
        }

        return GenerateAvatarResult(
            success = true,
            message = messageService.getMessage("profile.avatar.generate.success"),
            pictureUrl = avatarUrl
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
