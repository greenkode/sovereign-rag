package ai.sovereignrag.identity.core.invitation.command

import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.commons.notification.MessagePayload
import ai.sovereignrag.identity.commons.notification.MessageRecipient
import ai.sovereignrag.identity.commons.process.ProcessGateway
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.core.integration.CoreMerchantClient
import ai.sovereignrag.identity.core.invitation.dto.ResendInvitationCommand
import ai.sovereignrag.identity.core.invitation.dto.ResendInvitationResult
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

@Component
@Transactional
class ResendInvitationCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val clientRepository: OAuthRegisteredClientRepository,
    private val processGateway: ProcessGateway,
    private val coreMerchantClient: CoreMerchantClient,
    @Value("\${app.merchant.invitation.base-url}")
    private val invitationBaseUrl: String
) : Command.Handler<ResendInvitationCommand, ResendInvitationResult> {

    private val log = KotlinLogging.logger {}

    override fun handle(command: ResendInvitationCommand): ResendInvitationResult {
        log.info { "Processing resend invitation for email: ${command.userEmail}" }

        val resendingUser = userRepository.findById(UUID.fromString(command.resendByUserId))
            .orElseThrow { NotFoundException("Resending user not found") }

        val targetUser = userRepository.findByUsername(command.userEmail)
            ?: throw NotFoundException("User with email ${command.userEmail} not found")

        if (targetUser.emailVerified == true) {
            throw IllegalStateException("User has already completed the invitation process")
        }

        val merchant = clientRepository.findById(targetUser.merchantId.toString())
            .orElseThrow { NotFoundException("Merchant not found") }

        val existingProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            userId = targetUser.akuId!!,
            processType = ProcessType.MERCHANT_USER_INVITATION
        ) ?: throw NotFoundException("No pending invitation found for user")

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        coreMerchantClient.sendMessage(
            MessagePayload(
                listOf(MessageRecipient(address = targetUser.email)),
                "MERCHANT_USER_INVITATION", "EMAIL", "HIGH",
                mapOf(
                    "merchant_name" to merchant.clientName,
                    "invited_by" to "${resendingUser.firstName ?: ""} ${resendingUser.lastName ?: ""}".trim()
                        .ifEmpty { resendingUser.email },
                    "invitation_url" to "$invitationBaseUrl?token=${existingProcess.externalReference}",
                    "expiration_date" to formatter.format(
                        Instant.now().plusSeconds(existingProcess.type.timeInSeconds).atZone(
                            ZoneId.systemDefault()
                        ).toLocalDateTime()
                    )
                ), Locale.ENGLISH, UUID.randomUUID().toString(), "INDIVIDUAL"
            )
        )

        log.info { "Resent invitation successfully to ${targetUser.email}" }

        return ResendInvitationResult(
            success = true,
            message = "Invitation resent successfully"
        )
    }
}