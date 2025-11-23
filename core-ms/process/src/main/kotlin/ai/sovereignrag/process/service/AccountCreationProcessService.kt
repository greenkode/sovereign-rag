package ai.sovereignrag.process.service

import ai.sovereignrag.commons.accounting.AccountAddressDto
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.accounting.AddressPropertyDto
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID
import javax.money.Monetary

/**
 * Service handling account creation-specific business logic
 */
@Service
class AccountCreationProcessService(
    private val accountGateway: AccountGateway,
    private val processService: ProcessService,
) {

    private val log = KotlinLogging.logger {}

    fun completeAccountCreation(process: ProcessDto) {

        log.debug { "Processing AccountCreationAction for process: ${process.publicId}" }

        val request = process.getInitialRequest()

        val completed = process.getRequestByType(ProcessRequestType.CUSTOMER_INFORMATION_UPDATE)

        val currency = Monetary.getCurrency(request.data[ProcessRequestDataName.CURRENCY])

        accountGateway.findByUserIdAndCurrency(
            UUID.fromString(request.getStakeholderValue(ProcessStakeholderType.FOR_USER)), currency
        )?.let { account ->

            val newAddress = completed.getDataValue(ProcessRequestDataName.ACCOUNT_ADDRESS)
            val newAddressType = AccountAddressType.valueOf(completed.getDataValue(ProcessRequestDataName.ADDRESS_TYPE))
            val newPlatform = completed.getDataValue(ProcessRequestDataName.INTEGRATOR_ID)

            val addressAlreadyExists = account.addresses.any {
                it.address == newAddress &&
                it.type == newAddressType &&
                it.platform == newPlatform
            }

            if (!addressAlreadyExists) {
                val addressProperties = mutableSetOf<AddressPropertyDto>()

                accountGateway.addAddress(
                    account.publicId, AccountAddressDto(
                        newAddress,
                        newAddressType,
                        newPlatform,
                        Monetary.getCurrency(completed.data[ProcessRequestDataName.CURRENCY]),
                        addressProperties
                    )
                )
            } else {
                log.debug { "Address already exists for account ${account.publicId}: $newAddress ($newAddressType, $newPlatform)" }
            }
        }

        processService.completeProcess(process.publicId, request.id)
    }

    fun handleAccountCreationFailure(process: ProcessDto) {
        log.info { "Handling account creation failure for process: ${process.publicId}" }

        processService.failProcess(process.publicId)
    }

    fun handleAccountCreationExpiry(process: ProcessDto) {
        log.info { "Handling account creation expiry for process: ${process.publicId}" }

        processService.expireProcess(process.publicId, false)
    }
}