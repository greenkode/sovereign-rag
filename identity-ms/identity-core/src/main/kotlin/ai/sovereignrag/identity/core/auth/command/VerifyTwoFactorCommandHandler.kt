package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.TwoFactorCodeInvalidException
import ai.sovereignrag.identity.commons.exception.TwoFactorMaxAttemptsException
import ai.sovereignrag.identity.commons.exception.TwoFactorSessionInvalidException
import ai.sovereignrag.identity.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.service.CustomUserDetails
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.trusteddevice.dto.TrustDeviceCommand
import ai.sovereignrag.identity.core.trusteddevice.service.DeviceFingerprintService
import an.awesome.pipelinr.Command
import an.awesome.pipelinr.Pipeline
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

@Component
@Transactional
class VerifyTwoFactorCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val pipeline: Pipeline,
    private val deviceFingerprintService: DeviceFingerprintService,
    @Value("\${identity.2fa.max-attempts:3}") private val maxAttempts: Int = 3,
    @Value("\${identity.trusted-device.enabled:true}") private val trustedDeviceEnabled: Boolean = true,
    @Value("\${identity.trusted-device.default-duration-days:30}") private val defaultTrustDurationDays: Int = 30
) : Command.Handler<VerifyTwoFactorCommand, VerifyTwoFactorResult> {

    override fun handle(command: VerifyTwoFactorCommand): VerifyTwoFactorResult {

        log.info { "2FA verification attempt for session: ${command.sessionId}" }

        val process = processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, command.sessionId)
            ?: throw TwoFactorSessionInvalidException("Invalid session")

        val initialRequest = process.getInitialRequest()

        val storedCode = process.requests.maxByOrNull { it.id }?.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)
            ?: initialRequest.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)
            ?: throw TwoFactorSessionInvalidException("There is a problem with this session, please try again")

        val forUserId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)?.let { UUID.fromString(it) }
            ?: throw TwoFactorSessionInvalidException("There is a problem with this session, please try again")

        val storedDeviceFingerprint = initialRequest.getDataValueOrNull(ProcessRequestDataName.DEVICE_FINGERPRINT)

        val currentAttempts = 0

        if (currentAttempts >= maxAttempts) {

            log.warn { "Max attempts exceeded for 2FA verification" }

            processGateway.makeRequest(
                MakeProcessRequestPayload(
                    userId = forUserId,
                    processPublicId = process.publicId,
                    eventType = ProcessEvent.PROCESS_FAILED,
                    requestType = ProcessRequestType.FAIL_PROCESS,
                    channel = Channel.BUSINESS_WEB
                )
            )
            
            throw TwoFactorMaxAttemptsException("Maximum verification attempts exceeded")
        }

        if (storedCode != command.code) {

            log.warn { "Invalid 2FA code provided" }

            throw TwoFactorCodeInvalidException("Invalid verification code")
        }

        processGateway.completeProcess(process.publicId, initialRequest.id)

        val user = userRepository.findById(forUserId).orElseThrow {
            IllegalStateException("User not found")
        }

        val userDetails = CustomUserDetails(user)
        val accessToken = jwtTokenService.generateToken(user, userDetails)
        val refreshToken = refreshTokenService.createRefreshToken(
            user = user,
            ipAddress = command.ipAddress,
            userAgent = command.userAgent,
            deviceFingerprint = storedDeviceFingerprint
        )

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Two-factor authentication completed successfully",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    "processId" to process.publicId.toString(),
                    "username" to user.username,
                    "sessionId" to command.sessionId,
                    "code" to command.code,
                    "userId" to user.id.toString()
                )
            )
        )

        log.info { "2FA verification successful for user ${user.username}" }

        // Automatically trust device if enabled and device fingerprint is available
        if (trustedDeviceEnabled && storedDeviceFingerprint != null) {
            try {
                val trustResult = pipeline.send(
                    TrustDeviceCommand(
                        userId = user.id!!,
                        sessionId = command.sessionId,
                        deviceFingerprint = storedDeviceFingerprint,
                        deviceName = command.deviceName,
                        ipAddress = command.ipAddress,
                        userAgent = command.userAgent,
                        trustDurationDays = defaultTrustDurationDays
                    )
                )

                log.info { "Device automatically trusted for user ${user.username}: ${trustResult.message}" }
            } catch (e: Exception) {
                log.error(e) { "Failed to trust device for user ${user.username}" }
            }
        }

        return VerifyTwoFactorResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenService.getTokenExpirySeconds(),
            user = TwoFactorUserInfo(
                username = user.username,
                name = userDetails.getFullName(),
                email = user.email
            ),
            message = "Verification successful"
        )
    }
}