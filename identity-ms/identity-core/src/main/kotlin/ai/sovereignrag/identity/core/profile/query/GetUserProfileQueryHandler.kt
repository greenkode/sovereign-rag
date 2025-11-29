package ai.sovereignrag.identity.core.profile.query

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetUserProfileQueryHandler(
    private val userService: UserService,
    private val fileUploadGateway: FileUploadGateway
) : Command.Handler<GetUserProfileQuery, GetUserProfileResult> {

    companion object {
        private const val PRESIGNED_URL_EXPIRATION_MINUTES = 60L
    }

    override fun handle(command: GetUserProfileQuery): GetUserProfileResult {
        log.info { "Getting user profile" }

        val user = userService.getCurrentUser()

        val pictureUrl = user.pictureUrl?.let { resolveAvatarUrl(it) }

        return GetUserProfileResult(
            id = user.id.toString(),
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            pictureUrl = pictureUrl,
            locale = user.locale,
            emailVerified = user.emailVerified
        )
    }

    private fun resolveAvatarUrl(storedValue: String): String {
        return when {
            storedValue.startsWith("http") -> storedValue
            storedValue.startsWith("avatars/") -> fileUploadGateway.generatePresignedDownloadUrl(
                storedValue,
                PRESIGNED_URL_EXPIRATION_MINUTES
            )
            else -> storedValue
        }
    }
}
