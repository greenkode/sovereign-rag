package ai.sovereignrag.identity.core.settings.command

import an.awesome.pipelinr.Command
import java.math.BigDecimal

data class UpdateAlertsCommand(
    val failureLimit: BigDecimal,
    val lowBalance: BigDecimal
) : Command<UpdateAlertsResult>

data class UpdateAlertsResult(
    val success: Boolean,
    val message: String
)