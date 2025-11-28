package ai.sovereignrag.identity.core.profile.query

import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetUserProfileQueryHandler(
    private val userService: UserService
) : Command.Handler<GetUserProfileQuery, GetUserProfileResult> {

    override fun handle(command: GetUserProfileQuery): GetUserProfileResult {
        log.info { "Getting user profile" }

        val user = userService.getCurrentUser()

        return GetUserProfileResult(
            id = user.id.toString(),
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            pictureUrl = user.pictureUrl,
            locale = user.locale,
            emailVerified = user.emailVerified
        )
    }
}
