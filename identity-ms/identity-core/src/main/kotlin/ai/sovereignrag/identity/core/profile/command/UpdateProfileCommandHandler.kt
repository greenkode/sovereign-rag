package ai.sovereignrag.identity.core.profile.command

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
class UpdateProfileCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService
) : Command.Handler<UpdateProfileCommand, UpdateProfileResult> {

    override fun handle(command: UpdateProfileCommand): UpdateProfileResult {
        log.info { "Processing UpdateProfileCommand" }

        val user = userService.getCurrentUser()

        command.firstName?.let { user.firstName = it }
        command.lastName?.let { user.lastName = it }
        command.phoneNumber?.let { user.phoneNumber = it }
        command.locale?.let { user.locale = it }

        userRepository.save(user)

        log.info { "Updated profile for user: ${user.id}" }

        user.id?.let { userId ->
            cacheEvictionService.evictUserCaches(userId.toString())
            cacheEvictionService.evictUserDetailsCaches(userId.toString())
        }

        return UpdateProfileResult(
            success = true,
            message = messageService.getMessage("profile.update.success")
        )
    }
}
