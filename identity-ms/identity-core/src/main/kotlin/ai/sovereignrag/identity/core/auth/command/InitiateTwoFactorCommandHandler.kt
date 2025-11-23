package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.commons.Channel
import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditResource
import ai.sovereignrag.identity.commons.audit.IdentityType
import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.exception.EmailNotVerifiedException
import ai.sovereignrag.identity.commons.exception.ServerException
import ai.sovereignrag.identity.commons.exception.TwoFactorAuthenticationRequiredException
import ai.sovereignrag.identity.commons.process.CreateNewProcessPayload
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.auth.dto.DirectLoginResult
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.auth.service.TwoFactorEmailService
import ai.sovereignrag.identity.core.refreshtoken.service.RefreshTokenService
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.CustomUserDetails
import ai.sovereignrag.identity.core.service.TokenGenerationUtility
import ai.sovereignrag.identity.core.trusteddevice.service.DeviceFingerprintService
import ai.sovereignrag.identity.core.trusteddevice.service.TrustedDeviceService
import an.awesome.pipelinr.Command
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class InitiateTwoFactorCommandHandler(
    private val authenticationManager: AuthenticationManager,
    private val processGateway: ProcessGateway,
    private val accountLockoutService: AccountLockoutService,
    private val twoFactorEmailService: TwoFactorEmailService,
    private val tokenGenerationUtility: TokenGenerationUtility,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val trustedDeviceService: TrustedDeviceService,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    @Value("\${identity.2fa.token-length:6}") private val tokenLength: Int = 6,
    @Value("\${identity.2fa.skip-for-trusted-devices:true}") private val skipForTrustedDevices: Boolean = true
) : Command.Handler<InitiateTwoFactorCommand, DirectLoginResult> {

    override fun handle(command: InitiateTwoFactorCommand): DirectLoginResult {
        log.info { "2FA login attempt for user: ${command.username}" }

        try {

            val authToken = UsernamePasswordAuthenticationToken(command.username, command.password)
            val authentication = authenticationManager.authenticate(authToken)
            val userDetails = authentication.principal as CustomUserDetails
            val oauthUser = userDetails.getOAuthUser()

            accountLockoutService.handleSuccessfulLogin(command.username)

            if (!oauthUser.emailVerified)
                throw EmailNotVerifiedException()

            // Generate device fingerprint
            val deviceFingerprint = if (command.httpRequest != null) {
                deviceFingerprintService.generateFingerprintFromRequest(command.httpRequest, command.ipAddress)
            } else {
                command.deviceFingerprint ?: deviceFingerprintService.generateFingerprint(
                    command.userAgent,
                    command.ipAddress
                )
            }

            // Check if device is trusted
            val userId = oauthUser.id
            if (skipForTrustedDevices && userId != null) {
                val trustedDevice = trustedDeviceService.checkTrustedDevice(userId, deviceFingerprint)

                if (trustedDevice != null) {
                    log.info { "User ${oauthUser.username} logged in from trusted device" }

                    val accessToken = jwtTokenService.generateToken(oauthUser, userDetails)
                    val refreshToken = refreshTokenService.createRefreshToken(
                        user = oauthUser,
                        ipAddress = command.ipAddress,
                        userAgent = command.userAgent,
                        deviceFingerprint = deviceFingerprint
                    )

                    applicationEventPublisher.publishEvent(
                        AuditEvent(
                            actorId = userId.toString(),
                            actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim()
                                .ifEmpty { oauthUser.email },
                            merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                            identityType = IdentityType.USER,
                            resource = AuditResource.IDENTITY,
                            event = "Direct login from trusted device",
                            eventTime = Instant.now(),
                            timeRecorded = Instant.now(),
                            payload = mapOf(
                                "username" to oauthUser.username,
                                "ipAddress" to (command.ipAddress ?: "unknown"),
                                "trustedDeviceId" to (trustedDevice.id?.toString() ?: "unknown")
                            )
                        )
                    )

                    return DirectLoginResult(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = jwtTokenService.getTokenExpirySeconds(),
                        trustedDeviceId = trustedDevice.id?.toString(),
                        trustedUntil = trustedDevice.expiresAt
                    )
                }
            }

            val sessionId = RandomStringUtils.secure().nextAlphanumeric(100)
            val twoFactorCode = tokenGenerationUtility.generateNumericToken(tokenLength)

            val recentProcesses = processGateway.findRecentPendingProcessesByTypeAndForUserId(
                ProcessType.TWO_FACTOR_AUTH,
                userId!!,
                Instant.now().minusSeconds(60)
            )

            if (recentProcesses.isEmpty()) {
                processGateway.createProcess(
                    CreateNewProcessPayload(
                        userId = userId,
                        publicId = UUID.randomUUID(),
                        type = ProcessType.TWO_FACTOR_AUTH,
                        description = "Two Factor Authentication for ${oauthUser.email}",
                        initialState = ProcessState.PENDING,
                        requestState = ProcessState.COMPLETE,
                        channel = Channel.BUSINESS_WEB,
                        externalReference = sessionId,
                        data = mapOf(
                            ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                            ProcessRequestDataName.AUTHENTICATION_REFERENCE to twoFactorCode,
                            ProcessRequestDataName.DEVICE_FINGERPRINT to deviceFingerprint
                        ),
                        stakeholders = mapOf(
                            ProcessStakeholderType.FOR_USER to userId.toString()
                        )
                    )
                )

                twoFactorEmailService.sendTwoFactorCode(oauthUser, twoFactorCode, command.ipAddress)
            }

            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = userId.toString(),
                    actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim().ifEmpty { oauthUser.email },
                    merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "Two-factor authentication initiated",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        "username" to oauthUser.username,
                        "sessionId" to sessionId,
                        "ipAddress" to (command.ipAddress ?: "unknown"),
                        "userId" to userId.toString()
                    )
                )
            )

            log.info { "2FA code sent to user ${oauthUser.username}" }

            throw TwoFactorAuthenticationRequiredException(
                sessionId = sessionId,
                message = "Verification code sent!"
            )


        } catch (e: TwoFactorAuthenticationRequiredException) {
            throw e
        } catch (e: ClientException) {
            throw e
        } catch (e: AuthenticationException) {
            log.error(e) { "Failed 2FA login attempt for user: ${command.username}" }
            accountLockoutService.handleFailedLogin(command.username)
            throw ClientException("Invalid credentials")
        } catch (e: Exception) {
            log.error(e) { "Error during 2FA login for user: ${command.username}" }
            throw ServerException("An error occurred during login", e)
        }
    }

}