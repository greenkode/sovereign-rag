package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
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

        // Evict the KYC_USER and USER_DETAILS caches since user details have changed
        user.akuId?.let { akuId ->
            cacheEvictionService.evictUserCaches(akuId.toString())
            cacheEvictionService.evictUserDetailsCaches(akuId.toString())
        }

        return UpdateUserNameResult(
            success = true,
            message = "User name updated successfully"
        )
    }
}