package ai.sovereignrag.commons.pricing.dto

import ai.sovereignrag.commons.accounting.AccountDto
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class CreatePricingRequest(
    val accountType: AccountType,
    val accountPublicId: UUID? = null,
    val transactionType: TransactionType,
    val currency: CurrencyUnit,
    val validFrom: Instant,
    val validUntil: Instant? = null,
    val productId: UUID? = null,
    val integratorId: String? = null,
    val pricingData: Set<PricingDataRequest>
)

data class UpdatePricingRequest(
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
    val pricingData: Set<PricingDataRequest>? = null
)

data class PricingDataRequest(
    val pricingType: PricingType,
    val calculation: PricingCalculation,
    val value: BigDecimal,
    val expression: String? = null
) {
    companion object {
        fun fromApiRequest(apiRequest: PricingDataApiRequest): PricingDataRequest {
            return PricingDataRequest(
                pricingType = PricingType.valueOf(apiRequest.pricingType),
                calculation = PricingCalculation.valueOf(apiRequest.calculation),
                value = apiRequest.value,
                expression = apiRequest.expression
            )
        }
    }
}

data class PricingResponse(
    val publicId: UUID,
    val accountType: AccountType,
    val accountPublicId: UUID?,
    val transactionType: TransactionType,
    val currency: CurrencyUnit,
    val validFrom: Instant,
    val validUntil: Instant?,
    val integratorId: String?,
    val pricingData: Set<PricingDataResponse>
) {
    companion object {
        fun fromDto(dto: PricingDto): PricingResponse {
            return PricingResponse(
                publicId = dto.publicId,
                accountType = dto.accountType ?: throw IllegalStateException("Account type cannot be null"),
                accountPublicId = dto.accountPublicId,
                transactionType = dto.transactionType,
                currency = dto.currency ?: throw IllegalStateException("Currency cannot be null"),
                validFrom = dto.validFrom,
                validUntil = dto.validUntil,
                integratorId = dto.integratorId,
                pricingData = dto.data.map { PricingDataResponse.fromDto(it) }.toSet()
            )
        }
    }
}

data class PricingDataResponse(
    val pricingType: PricingType,
    val calculation: PricingCalculation,
    val value: BigDecimal,
    val expression: String?
) {
    companion object {
        fun fromDto(dto: PricingDataDto): PricingDataResponse {
            return PricingDataResponse(
                pricingType = dto.pricingType,
                calculation = dto.calculation,
                value = dto.value,
                expression = dto.expression
            )
        }
    }
}

data class PricingSearchRequest(
    val accountType: AccountType? = null,
    val transactionType: TransactionType? = null,
    val accountPublicId: UUID? = null,
    val productId: UUID? = null,
    val integratorId: String? = null,
    val active: Boolean = false,
    val pageable: Pageable
)

data class PricingDto(
    val id: Long,
    val publicId: UUID,
    val accountType: AccountType?,
    val accountPublicId: UUID?,
    val transactionType: TransactionType,
    val currency: CurrencyUnit?,
    val validFrom: Instant,
    val validUntil: Instant?,
    val integratorId: String?,
    val data: Set<PricingDataDto>
)

data class PricingDataDto(
    val pricingType: PricingType,
    val calculation: PricingCalculation,
    val value: BigDecimal,
    val expression: String?
)

data class PricingCalculationDto(
    val fee: MonetaryAmount,
    val commission: MonetaryAmount,
    val vat: MonetaryAmount,
    val rebate: MonetaryAmount
) : java.io.Serializable

// API v1 DTOs for pricing management endpoints

data class PricingRuleApiRequest(
    val transactionType: String,
    val currency: String,
    val accountType: String?,
    val accountPublicId: UUID?,
    val integratorId: String?,
    val productId: UUID?,
    val validFrom: Instant,
    val validUntil: Instant?,
    val pricingData: List<PricingDataApiRequest>
) {
    companion object {
        fun toCreateRequest(apiRequest: PricingRuleApiRequest): CreatePricingRequest {
            return CreatePricingRequest(
                accountType = apiRequest.accountType?.let { AccountType.valueOf(it) } ?: AccountType.BUSINESS,
                accountPublicId = apiRequest.accountPublicId,
                transactionType = TransactionType.valueOf(apiRequest.transactionType),
                currency = javax.money.Monetary.getCurrency(apiRequest.currency),
                validFrom = apiRequest.validFrom,
                validUntil = apiRequest.validUntil,
                productId = apiRequest.productId,
                integratorId = apiRequest.integratorId,
                pricingData = apiRequest.pricingData.map { PricingDataRequest.fromApiRequest(it) }.toSet()
            )
        }
    }
}

data class UpdatePricingRuleApiRequest(
    val transactionType: String?,
    val currency: String?,
    val accountType: String?,
    val accountPublicId: UUID?,
    val integratorId: String?,
    val productId: UUID?,
    val validFrom: Instant?,
    val validUntil: Instant?,
    val version: Int?,
    val pricingData: List<PricingDataApiRequest>?
) {
    companion object {
        fun toUpdateRequest(apiRequest: UpdatePricingRuleApiRequest): UpdatePricingRequest {
            return UpdatePricingRequest(
                validFrom = apiRequest.validFrom,
                validUntil = apiRequest.validUntil,
                pricingData = apiRequest.pricingData?.map { PricingDataRequest.fromApiRequest(it) }?.toSet()
            )
        }
    }
}

data class PricingDataApiRequest(
    val pricingType: String,
    val calculation: String,
    val value: BigDecimal,
    val expression: String?
)

data class PricingDataApiResponse(
    val pricingType: String,
    val calculation: String,
    val value: BigDecimal,
    val expression: String?
) {
    companion object {
        fun fromDto(dto: PricingDataDto): PricingDataApiResponse {
            return PricingDataApiResponse(
                pricingType = dto.pricingType.name,
                calculation = dto.calculation.name,
                value = dto.value,
                expression = dto.expression
            )
        }
    }
}

data class PricingDtoResult(
    val publicId: UUID,
    val accountType: AccountType?,
    val accountPublicId: UUID?,
    val accountName: String?,
    val transactionType: TransactionType,
    val currency: CurrencyUnit?,
    val validFrom: Instant,
    val validUntil: Instant?,
    val integratorId: String?,
    val integratorName: String?,
    val data: Set<PricingDataDto>
) {
    companion object {

        fun fromDto(dto: PricingDto, account: AccountDto?) =

            PricingDtoResult(
                dto.publicId,
                dto.accountType,
                dto.accountPublicId,
                account?.name,
                dto.transactionType,
                dto.currency,
                dto.validFrom,
                dto.validUntil,
                null,
                null,
                dto.data
            )

    }
}