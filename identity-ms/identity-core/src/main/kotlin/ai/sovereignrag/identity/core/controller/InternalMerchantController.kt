package ai.sovereignrag.identity.core.controller

import ai.sovereignrag.identity.commons.dto.UpdateMerchantEnvironmentResponse
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.service.MerchantService
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    ): UpdateMerchantEnvironmentResponse {
        log.info { "Internal request to update merchant environment for: $merchantId" }

        return merchantService.updateMerchantEnvironment(UUID.fromString(merchantId), request.environmentMode)
            .let { result ->
                UpdateMerchantEnvironmentResponse(
                    merchantId = result.merchantId,
                    environmentMode = result.environmentMode.name,
                    lastModifiedAt = result.lastModifiedAt,
                    affectedUsers = result.affectedUsers
                )
            }
    }
}

data class UpdateMerchantEnvironmentRequest(
    @JsonProperty("environmentMode")
    val environmentMode: EnvironmentMode
)
