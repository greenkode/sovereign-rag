package ai.sovereignrag.pricing.spi

import ai.sovereignrag.commons.cache.CacheNames
import ai.sovereignrag.commons.currency.DefaultCurrency.currency
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.performance.LogExecutionTime
import ai.sovereignrag.commons.pricing.PricingGateway
import ai.sovereignrag.commons.pricing.dto.*
import ai.sovereignrag.commons.pricing.dto.PricingType
import ai.sovereignrag.integration.billpay.dao.BillPayProductRepository
import ai.sovereignrag.pricing.domain.model.PricingDataEntity
import ai.sovereignrag.pricing.domain.model.PricingDataRepository
import ai.sovereignrag.pricing.domain.model.PricingEntity
import ai.sovereignrag.pricing.domain.model.PricingRepository
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PricingService(
    private val entityManager: EntityManager,
    private val pricingRepository: PricingRepository,
    private val pricingDataRepository: PricingDataRepository,
    private val billPayProductRepository: BillPayProductRepository
) : PricingGateway {

    private val log = logger {}

    @LogExecutionTime
    @Cacheable(
        cacheNames = [CacheNames.PRICING_CALC],
        key = "'pricing_' + #parameters.transactionType.name + '_' + #parameters.accountCharged.type.name + '_' + #parameters.accountCharged.currency.currencyCode + '_' + (#parameters.integratorId ?: 'null') + '_' + (#parameters.productId?.toString() ?: 'null') + '_' + #parameters.amount.number.toString()",
        unless = "#result == null"
    )
    override fun calculateFees(parameters: PricingParameterDto): PricingCalculationDto? {

        log.info { "Transaction Type: ${parameters.transactionType}, Account Type: ${parameters.accountCharged.type}, Currency: ${parameters.accountCharged.currency.currencyCode} Account Charged: ${parameters.accountCharged.publicId}, Transaction Time: ${parameters.transactionTime}, Integrator Id: ${parameters.integratorId}, Product Id: ${parameters.productId} Amount: ${parameters.amount} " }

        return (entityManager
            .createNativeQuery(
                "SELECT * FROM core.get_valid_pricing(:transactionType, :currency, :transactionDate, :accountType, :accountPublicId, :integratorId, :productId)",
                PricingEntity::class.java
            ).setParameter("transactionType", parameters.transactionType.name)
            .setParameter("currency", parameters.accountCharged.currency.currencyCode)
            .setParameter("transactionDate", parameters.transactionTime)
            .setParameter("accountType", parameters.accountCharged.type.name)
            .setParameter("accountPublicId", parameters.accountCharged.publicId)
            .setParameter("integratorId", parameters.integratorId)
            .setParameter("productId", parameters.productId)
            .singleResult as PricingEntity?)?.toDomain()
            ?.let { pricing ->

                val fee = pricing.getPriceByType(PricingType.FEE, parameters.amount)

                val commission = pricing.getPriceByType(PricingType.COMMISSION, parameters.amount)

                val rebate = pricing.getPriceByType(PricingType.REBATE, parameters.amount)

                val vat = pricing.getPriceByType(PricingType.VAT, fee)

                PricingCalculationDto(fee, commission, vat, rebate)
            }
    }

    @Transactional
    override fun createPricing(request: CreatePricingRequest): PricingDto {
        log.info { "Creating pricing for ${request.accountType} - ${request.transactionType}" }

        // Validate that the new pricing doesn't overlap with existing pricings
        validateNoPricingOverlap(request)

        val pricing = PricingEntity(
            publicId = UUID.randomUUID(),
            accountType = request.accountType,
            accountPublicId = request.accountPublicId,
            transactionType = request.transactionType,
            currency = request.currency,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            productId = request.productId,
            integratorId = request.integratorId?.trim().takeIf { !it.isNullOrBlank() }
        )

        val savedPricing = pricingRepository.save(pricing)

        val pricingDataEntities = request.pricingData.map { data ->
            PricingDataEntity(
                pricingType = data.pricingType,
                calculation = data.calculation,
                value = data.value,
                expression = data.expression,
                pricing = savedPricing
            )
        }

        pricingDataRepository.saveAll(pricingDataEntities)

        val savedEntity = pricingRepository.findByPublicId(savedPricing.publicId)!!
        val productProjection = savedEntity.productId?.let { billPayProductRepository.findProjectionByPublicId(it) }
        return savedEntity.toDto(productProjection?.publicId, productProjection?.name)
    }

    @Transactional
    override fun updatePricing(publicId: UUID, request: UpdatePricingRequest): PricingDto {
        log.info { "Updating pricing $publicId (immutable - will expire existing and create new)" }

        val existingPricing = pricingRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException("Pricing not found: $publicId")

        // First, expire the existing pricing by setting validUntil to now
        val expiredPricing = PricingEntity(
            publicId = existingPricing.publicId,
            accountType = existingPricing.accountType,
            accountPublicId = existingPricing.accountPublicId,
            transactionType = existingPricing.transactionType,
            currency = existingPricing.currency,
            validFrom = existingPricing.validFrom,
            validUntil = Instant.now(), // Expire the current pricing
            productId = existingPricing.productId,
            integratorId = existingPricing.integratorId,
            data = existingPricing.data,
            id = existingPricing.id
        )

        pricingRepository.save(expiredPricing)

        // Create a new pricing entity with updated values and new UUID
        val newPricing = PricingEntity(
            publicId = UUID.randomUUID(), // New UUID for the new pricing
            accountType = existingPricing.accountType,
            accountPublicId = existingPricing.accountPublicId,
            transactionType = existingPricing.transactionType,
            currency = existingPricing.currency,
            validFrom = request.validFrom ?: Instant.now(),
            validUntil = request.validUntil,
            productId = existingPricing.productId,
            integratorId = existingPricing.integratorId
        )

        val savedNewPricing = pricingRepository.save(newPricing)

        // Create pricing data for the new pricing
        val pricingDataToUse = request.pricingData ?: existingPricing.data.map { existingData ->
            PricingDataRequest(
                pricingType = existingData.toDomain().pricingType,
                calculation = existingData.toDomain().calculation,
                value = existingData.toDomain().value,
                expression = existingData.toDomain().expression
            )
        }.toSet()

        val newPricingDataEntities = pricingDataToUse.map { data ->
            PricingDataEntity(
                pricingType = data.pricingType,
                calculation = data.calculation,
                value = data.value,
                expression = data.expression,
                pricing = savedNewPricing
            )
        }

        pricingDataRepository.saveAll(newPricingDataEntities)

        log.info { "Expired pricing $publicId and created new pricing ${savedNewPricing.publicId}" }

        val updatedEntity = pricingRepository.findByPublicId(savedNewPricing.publicId)!!
        val productProjection = updatedEntity.productId?.let { billPayProductRepository.findProjectionByPublicId(it) }
        return updatedEntity.toDto(productProjection?.publicId, productProjection?.name)
    }

    override fun getPricing(publicId: UUID): PricingDto {
        log.info { "Getting pricing $publicId" }

        val pricing = pricingRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException("Pricing not found: $publicId")

        val productProjection = pricing.productId?.let { billPayProductRepository.findProjectionByPublicId(it) }
        return pricing.toDto(productProjection?.publicId, productProjection?.name)
    }

    @Transactional
    override fun deletePricing(publicId: UUID) {
        log.info { "Deleting pricing $publicId (soft delete - will expire the pricing)" }

        val pricing = pricingRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException("Pricing not found: $publicId")

        if (pricing.validUntil != null && pricing.validUntil.isBefore(Instant.now())) {
            log.warn { "Pricing $publicId is already expired" }
            return
        }

        val expiredPricing = PricingEntity(
            publicId = pricing.publicId,
            accountType = pricing.accountType,
            accountPublicId = pricing.accountPublicId,
            transactionType = pricing.transactionType,
            currency = pricing.currency,
            validFrom = pricing.validFrom,
            validUntil = Instant.now(), // Expire the pricing
            productId = pricing.productId,
            integratorId = pricing.integratorId,
            data = pricing.data,
            id = pricing.id
        )

        pricingRepository.save(expiredPricing)

        log.info { "Expired pricing $publicId at ${Instant.now()}" }
    }

    override fun searchPricing(request: PricingSearchRequest): Page<PricingDto> {
        log.info { "Searching pricing with filters: $request" }

        val accountType = request.accountType
        val transactionType = request.transactionType

        val pricingPage = if (request.active) {
            if (accountType == null || transactionType == null) {
                throw IllegalArgumentException("accountType and transactionType are required when searching for active pricing")
            }
            val validPricing = pricingRepository.findValidPricing(
                accountType = accountType,
                transactionType = transactionType,
                accountPublicId = request.accountPublicId,
                productId = request.productId,
                integratorId = request.integratorId,
                date = Instant.now(),
                page = request.pageable
            )
            val productProjectionsMap = getProductProjectionsMapForEntities(validPricing)
            validPricing.map { entity ->
                val projection = productProjectionsMap[entity.productId]
                entity.toDto(projection?.publicId, projection?.name)
            }
        } else {
            pricingRepository.findUnexpiredPricingByFilters(
                accountType = accountType,
                transactionType = transactionType,
                accountPublicId = request.accountPublicId,
                productId = request.productId,
                integratorId = request.integratorId,
                pageable = request.pageable
            ).let { page ->
                val productProjectionsMap = getProductProjectionsMapForEntities(page)
                page.map { entity ->
                    val projection = productProjectionsMap[entity.productId]
                    entity.toDto(projection?.publicId, projection?.name)
                }
            }
        }

        return pricingPage
    }

    override fun getAllActivePricing(activeOn: Instant, pageable: Pageable): Page<PricingDto> {
        log.info { "Fetching all active pricing rules as of $activeOn with pagination" }

        val pricingPage = pricingRepository.findActivePricing(activeOn, pageable)
        val productProjectionsMap = getProductProjectionsMapForEntities(pricingPage)

        return pricingPage.map { entity ->
            val projection = productProjectionsMap[entity.productId]
            entity.toDto(projection?.publicId, projection?.name)
        }
    }

    private fun getProductProjectionsMapForEntities(pricingEntities: Page<PricingEntity>) =
        getProductProjectionsMapForEntities(pricingEntities.content)

    private fun getProductProjectionsMapForEntities(pricingEntities: List<PricingEntity>): Map<UUID, ai.sovereignrag.integration.billpay.dao.BillPayProductProjection> {
        val productIds = pricingEntities.mapNotNull { it.productId }.distinct()
        return if (productIds.isNotEmpty()) {
            billPayProductRepository.findProjectionsByPublicIds(productIds)
                .associateBy { it.publicId }
        } else {
            emptyMap()
        }
    }

    /**
     * Validates that the new pricing doesn't overlap with existing active pricings
     */
    private fun validateNoPricingOverlap(request: CreatePricingRequest) {
        log.debug { "Validating pricing overlap for ${request.accountType} - ${request.transactionType}" }

        val overlappingPricings = pricingRepository.findOverlappingPricing(
            accountType = request.accountType,
            transactionType = request.transactionType,
            accountPublicId = request.accountPublicId,
            productId = request.productId,
            integratorId = request.integratorId,
            validFrom = request.validFrom,
            validUntil = request.validUntil ?: Instant.ofEpochMilli(Long.MAX_VALUE) // Treat null as far future
        )

        if (overlappingPricings.isNotEmpty()) {
            val conflictingPricing = overlappingPricings.first()
            val errorMessage = buildString {
                append("Pricing overlap detected. ")
                append("New pricing (${request.validFrom} to ${request.validUntil ?: "unlimited"}) ")
                append("conflicts with existing pricing ${conflictingPricing.publicId} ")
                append("(${conflictingPricing.validFrom} to ${conflictingPricing.validUntil ?: "unlimited"}) ")
                append("for the same criteria: ")
                append("accountType=${request.accountType}, ")
                append("transactionType=${request.transactionType}")

                if (request.accountPublicId != null) {
                    append(", accountPublicId=${request.accountPublicId}")
                }
                if (request.productId != null) {
                    append(", productId=${request.productId}")
                }
                if (request.integratorId != null) {
                    append(", integratorId=${request.integratorId}")
                }
            }

            log.warn { errorMessage }
            throw IllegalArgumentException(errorMessage)
        }

        log.debug { "No pricing overlap detected - validation passed" }
    }
}