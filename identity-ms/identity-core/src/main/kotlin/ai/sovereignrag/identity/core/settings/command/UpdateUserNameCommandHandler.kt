package ai.sovereignrag.identity.core.settings.command

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
class UpdateUserNameCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val cacheEvictionService: CacheEvictionService
) : Command.Handler<UpdateUserNameCommand, UpdateUserNameResult> {

    override fun handle(command: UpdateUserNameCommand): UpdateUserNameResult {
        log.info { "Processing UpdateUserNameCommand" }

        val user = userService.getCurrentUser()

        user.firstName = command.firstName
        user.lastName = command.lastName

        userRepository.save(user)

        log.info { "Updated user name for user: ${user.id}" }

        user.id?.let { userId ->
            cacheEvictionService.evictUserCaches(userId.toString())
            cacheEvictionService.evictUserDetailsCaches(userId.toString())
        }

        return UpdateUserNameResult(
            success = true,
            message = "User name updated successfully"
        )
    }
}