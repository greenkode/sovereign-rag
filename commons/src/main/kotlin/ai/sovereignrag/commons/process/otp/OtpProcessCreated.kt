package ai.sovereignrag.commons.process.otp

import ai.sovereignrag.commons.notification.enumeration.NotificationChannel
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.commons.user.dto.UserDetailsDto
import ai.sovereignrag.commons.user.dto.UserType
import java.util.UUID

data class OtpProcessCreated(
    val processId: UUID,
    val template: TemplateName,
    val notificationChannel: NotificationChannel,
    val processType: ProcessType,
    val userDetailsDto: UserDetailsDto,
    val accountType: UserType,
    val processRequestData: Map<ProcessRequestDataName, String>
)