package ai.sovereignrag.commons.pricing

import ai.sovereignrag.commons.pricing.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

interface PricingGateway {

    fun calculateFees(parameters: PricingParameterDto) : PricingCalculationDto?

    fun createPricing(request: CreatePricingRequest): PricingDto

    fun updatePricing(publicId: UUID, request: UpdatePricingRequest): PricingDto

    fun getPricing(publicId: UUID): PricingDto

    fun deletePricing(publicId: UUID)

    fun searchPricing(request: PricingSearchRequest): Page<PricingDto>

    fun getAllActivePricing(activeOn: Instant, pageable: Pageable): Page<PricingDto>
}