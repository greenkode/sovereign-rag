package ai.sovereignrag.identity.core.unit

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.commons.fileupload.FileUploadResult
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.profile.command.GenerateAvatarCommand
import ai.sovereignrag.identity.core.profile.command.GenerateAvatarCommandHandler
import ai.sovereignrag.identity.core.profile.dto.AvatarStyle
import ai.sovereignrag.identity.core.profile.service.AvatarGenerationResult
import ai.sovereignrag.identity.core.profile.service.AvatarImageGenerationService
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
import java.io.InputStream
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
    private val avatarImageGenerationService: AvatarImageGenerationService = mockk()
    private val fileUploadGateway: FileUploadGateway = mockk()

    private lateinit var handler: GenerateAvatarCommandHandler

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        handler = GenerateAvatarCommandHandler(
            userService,
            userRepository,
            cacheEvictionService,
            messageService,
            avatarImageGenerationService,
            fileUploadGateway
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
        assertNotNull(result.pictureUrl)
        assertTrue(result.pictureUrl!!.contains("ui-avatars.com"))
        assertTrue(result.pictureUrl!!.contains("FF5733"))

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
        assertNotNull(result.pictureUrl)
        assertTrue(result.pictureUrl!!.contains("dicebear.com"))
        assertTrue(result.pictureUrl!!.contains("avataaars"))
    }

    @Test
    fun `should generate AI avatar successfully`() {
        val user = createMockUser()
        val imageBytes = "fake-image-data".toByteArray()
        val uploadedUrl = "https://s3.example.com/avatars/user-123.png"

        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "A professional business person"
        )

        every { userService.getCurrentUser() } returns user
        every { avatarImageGenerationService.isConfigured() } returns true
        every { avatarImageGenerationService.generateAvatar(any()) } returns AvatarGenerationResult(
            success = true,
            errorMessage = null,
            imageBytes = imageBytes
        )
        every { fileUploadGateway.uploadUserFile(any<InputStream>(), any(), any(), any(), any(), any()) } returns FileUploadResult(
            key = "avatars/user-123.png",
            url = uploadedUrl,
            contentType = "image/png",
            size = imageBytes.size.toLong()
        )
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        assertEquals("Avatar generated successfully", result.message)
        assertEquals(uploadedUrl, result.pictureUrl)

        verify { avatarImageGenerationService.generateAvatar("A professional business person") }
        verify { fileUploadGateway.uploadUserFile(any<InputStream>(), any(), "image/png", imageBytes.size.toLong(), "avatars", userId) }
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
        every { messageService.getMessage("profile.avatar.ai.generation.failed") } returns "AI generation failed"

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("AI generation failed", result.message)
    }

    @Test
    fun `should fail AI avatar generation when not configured`() {
        val user = createMockUser()
        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "A professional person"
        )

        every { userService.getCurrentUser() } returns user
        every { avatarImageGenerationService.isConfigured() } returns false
        every { messageService.getMessage("profile.avatar.ai.generation.failed") } returns "AI generation failed"

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("AI generation failed", result.message)
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
        every { avatarImageGenerationService.isConfigured() } returns true
        every { avatarImageGenerationService.generateAvatar(any()) } returns AvatarGenerationResult(
            success = false,
            errorMessage = "OpenAI API error",
            imageBytes = null
        )
        every { messageService.getMessage("profile.avatar.ai.generation.failed") } returns "AI generation failed"

        val result = handler.handle(command)

        assertFalse(result.success)
        assertEquals("AI generation failed", result.message)
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
    fun `should delete old avatar when generating AI avatar`() {
        val oldAvatarUrl = "https://s3.example.com/sovereign-rag/avatars/user-123/old-avatar.png"
        val user = createMockUser(pictureUrl = oldAvatarUrl)
        val imageBytes = "fake-image-data".toByteArray()
        val newUploadedUrl = "https://s3.example.com/avatars/user-123-new.png"

        val command = GenerateAvatarCommand(
            style = AvatarStyle.AI_GENERATED,
            backgroundColor = null,
            prompt = "A professional business person"
        )

        every { userService.getCurrentUser() } returns user
        every { avatarImageGenerationService.isConfigured() } returns true
        every { avatarImageGenerationService.generateAvatar(any()) } returns AvatarGenerationResult(
            success = true,
            errorMessage = null,
            imageBytes = imageBytes
        )
        every { fileUploadGateway.deleteFile(any()) } just runs
        every { fileUploadGateway.uploadUserFile(any<InputStream>(), any(), any(), any(), any(), any()) } returns FileUploadResult(
            key = "avatars/user-123-new.png",
            url = newUploadedUrl,
            contentType = "image/png",
            size = imageBytes.size.toLong()
        )
        every { userRepository.save(any()) } returns user
        every { cacheEvictionService.evictUserCaches(any()) } just runs
        every { cacheEvictionService.evictUserDetailsCaches(any()) } just runs
        every { messageService.getMessage("profile.avatar.generate.success") } returns "Avatar generated successfully"

        val result = handler.handle(command)

        assertTrue(result.success)
        verify { fileUploadGateway.deleteFile(any()) }
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
