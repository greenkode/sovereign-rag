package ai.sovereignrag.identity.core.auth.command

import ai.sovereignrag.identity.core.auth.dto.DirectLoginResult
import an.awesome.pipelinr.Command
import jakarta.servlet.http.HttpServletRequest

data class InitiateTwoFactorCommand(
    val username: String,
    val password: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val deviceFingerprint: String? = null,
    val httpRequest: HttpServletRequest? = null
) : Command<DirectLoginResult>