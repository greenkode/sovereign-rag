package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.profile.command.GenerateAvatarCommand
import ai.sovereignrag.identity.core.profile.command.GenerateAvatarCommandHandler
import ai.sovereignrag.identity.core.profile.dto.AvatarStyle
import ai.sovereignrag.identity.core.profile.service.AvatarGenerationProcessResult
import ai.sovereignrag.identity.core.profile.service.AvatarGenerationProcessService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CacheEvictionService
import ai.sovereignrag.identity.core.service.UserService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenerateAvatarCommandHandlerTest {

    private val userService: UserService = mockk()
    private val userRepository: OAuthUserRepository = mockk()
    private val cacheEvictionService: CacheEvictionService = mockk()
    private val messageService: MessageService = mockk()
    private val fileUploadGateway: FileUploadGateway = mockk()
    private val avatarGenerationProcessService: AvatarGenerationProcessService = mockk()

    private lateinit var handler: GenerateAvatarCommandHandler

    private val userId = UUID.randomUUID()
    private val processId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = GenerateAvatarCommandHandler(
            userService,
            userRepository,
            cacheEvictionService,
            messageService,
            fileUploadGateway,
            avatarGenerationProcessService
        )
    }

    @Test
    fun `should generate initials avatar successfully`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.INITIALS,
            backgroundColor = "#FF5733",
            prompt = null
        )

        every { userService.getCurrentUser() } returns user
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Avatar generated successfully", result.message)
        val pictureUrl = result.pictureUrl
        assertNotNull(pictureUrl)
        assertTrue(pictureUrl.contains("ui-avatars.com"))
        assertTrue(pictureUrl.contains("FF5733"))

        verify { userRepository.save(user) }
        verify { cacheEvictionService.evictUserCaches(userId.toString()) }
    }

    @Test
    fun `should generate dicebear avataaars style`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.DICEBEAR_AVATAAARS,
            backgroundColor = null,
            prompt = null
        )

        every { userService.getCurrentUser() } returns user
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        val pictureUrl = result.pictureUrl
        assertNotNull(pictureUrl)
        assertTrue(pictureUrl.contains("dicebear.com"))
        assertTrue(pictureUrl.contains("avataaars"))
    }

    @Test
    fun `should generate AI avatar successfully`() {
        val previousPictureUrl = "avatars/old-avatar.png"
        val user = createMockUser(previousPictureUrl)
        val imageUrl = "https://s3.example.com/avatars/user-123.png"
        val imageKey = "avatars/user-123.png"

        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "A professional business person"
        )

        every { userService.getCurrentUser() } returns user
        every { avatarGenerationProcessService.generateAvatar(userId, "A professional business person", null, previousPictureUrl) } returns AvatarGenerationProcessResult(
            success = true,
            message = null,
            imageKey = imageKey,
            imageUrl = imageUrl,
            processId = processId
        )
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Avatar generated successfully", result.message)
        assertEquals(imageUrl, result.pictureUrl)
        assertEquals(processId, result.processId)
        assertEquals(imageKey, result.imageKey)

        verify { avatarGenerationProcessService.generateAvatar(userId, "A professional business person", null, previousPictureUrl) }
        verify { user.pictureUrl = imageKey }
        verify { userRepository.save(user) }
        verify { cacheEvictionService.evictUserCaches(userId.toString()) }
        verify { cacheEvictionService.evictUserDetailsCaches(userId.toString()) }
    }

    @Test
    fun `should generate AI avatar with existing process ID`() {
        val user = createMockUser()
        val imageUrl = "https://s3.example.com/avatars/user-123-v2.png"
        val imageKey = "avatars/user-123-v2.png"
        val existingProcessId = UUID.randomUUID()

        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "Make the hair longer",
            processId = existingProcessId
        )

        every { userService.getCurrentUser() } returns user
        every { avatarGenerationProcessService.generateAvatar(userId, "Make the hair longer", existingProcessId, null) } returns AvatarGenerationProcessResult(
            success = true,
            message = null,
            imageKey = imageKey,
            imageUrl = imageUrl,
            processId = existingProcessId
        )
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals(existingProcessId, result.processId)

        verify { avatarGenerationProcessService.generateAvatar(userId, "Make the hair longer", existingProcessId, null) }
        verify { user.pictureUrl = imageKey }
        verify { userRepository.save(user) }
    }

    @Test
    fun `should fail AI avatar generation when prompt is missing`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = null
        )

        every { userService.getCurrentUser() } returns user
        every { messageService.getMessage("profile.avatar.ai.prompt.required") } returns "A text description is required"

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("A text description is required", result.message)
    }

    @Test
    fun `should fail AI avatar generation when prompt is blank`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "   "
        )

        every { userService.getCurrentUser() } returns user
        every { messageService.getMessage("profile.avatar.ai.prompt.required") } returns "A text description is required"

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("A text description is required", result.message)
    }

    @Test
    fun `should fail AI avatar generation when service returns error`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "A professional person"
        )

        every { userService.getCurrentUser() } returns user
        every { avatarGenerationProcessService.generateAvatar(userId, "A professional person", null, null) } returns AvatarGenerationProcessResult(
            success = false,
            message = "AI avatar generation is not configured",
            imageKey = null,
            imageUrl = null,
            processId = processId
        )

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("AI avatar generation is not configured", result.message)
        assertEquals(processId, result.processId)
    }

    @Test
    fun `should fail when user has no ID`() {
        val user = mockk<OAuthUser>()

        every { user.id } returns null
        every { userService.getCurrentUser() } returns user
        every { messageService.getMessage("profile.avatar.no_user") } returns "User not found"

        val command = GenerateAvatarCommand(
            style = AvatarStyle.INITIALS,
            backgroundColor = null,
            prompt = null
        )

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("User not found", result.message)
    }

    @Test
    fun `should use default background color for initials avatar`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.INITIALS,
            backgroundColor = null,
            prompt = null
        )

        every { userService.getCurrentUser() } returns user
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        val pictureUrl = result.pictureUrl
        assertNotNull(pictureUrl)
        assertTrue(pictureUrl.contains("6366f1"))
    }

    @Test
    fun `should generate all dicebear styles`() {
        val styles = listOf(
            AvatarStyle.DICEBEAR_BOTTTS to "bottts",
            AvatarStyle.DICEBEAR_IDENTICON to "identicon",
            AvatarStyle.DICEBEAR_SHAPES to "shapes"
        )

        styles.forEach { (style, expectedStyleName) ->
            val user = createMockUser()
            val command = GenerateAvatarCommand(
                style = style,
                backgroundColor = null,
                prompt = null
            )

            every { userService.getCurrentUser() } returns user
            every { userRepository.save(any()) } returns user
            every { cacheEvictionService.evictUserCaches(any()) } just runs
            every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
            every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

            val result = handler.handle(command)

            assertTrue(result.success, "Failed for style: $style")
            val pictureUrl = result.pictureUrl
            assertNotNull(pictureUrl)
            assertTrue(pictureUrl.contains(expectedStyleName), "URL should contain $expectedStyleName for style $style")
        }
    }

    private fun createMockUser(pictureUrl: String? = null): OAuthUser {
        val user = mockk<OAuthUser>(relaxed = true)
        every { user.id } returns userId
        every { user.firstName } returns "John"
        every { user.lastName } returns "Doe"
        every { user.fullName() } returns "John Doe"
        every { user.pictureUrl } returns pictureUrl
        return user
    }
}
