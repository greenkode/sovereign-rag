package ai.sovereignrag.commons.banking

import ai.sovereignrag.commons.user.dto.MerchantDetailsDto
import java.util.UUID
import javax.money.CurrencyUnit

interface BankingGateway {

    fun getAccountDetails(integrationId: String, bankCode: String, accountNumber: String): BankingCustomerDetailsResult

    fun createAccount(
        customerIdentifier: String,
        merchantDetails: MerchantDetailsDto,
        parameters: Map<String, Any>,
        publicId: UUID,
        currency: CurrencyUnit,
        currencyIssuer: String? = null,
    ): AccountDetails

    fun getTransactionStatus(reference: String, integratorId: String, internalReference: UUID): BankingTransactionStatusResult

}