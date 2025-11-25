package ai.sovereignrag.identity.core.controller

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.service.MerchantService
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/internal/merchants")
class InternalMerchantController(
    private val merchantService: MerchantService
) {

    @PutMapping("/{merchantId}/environment")
    fun updateMerchantEnvironment(
        @PathVariable merchantId: String,
        @RequestBody request: UpdateMerchantEnvironmentRequest
    ): Map<String, Any> {
        log.info { "Internal request to update merchant environment for: $merchantId" }

        val result = merchantService.updateMerchantEnvironment(merchantId, request.environmentMode)

        return mapOf(
            "merchantId" to result.merchantId,
            "environmentMode" to result.environmentMode,
            "lastModifiedAt" to result.lastModifiedAt.toString(),
            "affectedUsers" to result.affectedUsers
        )
    }
}

data class UpdateMerchantEnvironmentRequest(
    @JsonProperty("merchantId")
    val merchantId: String,

    @JsonProperty("environmentMode")
    val environmentMode: EnvironmentMode
)
