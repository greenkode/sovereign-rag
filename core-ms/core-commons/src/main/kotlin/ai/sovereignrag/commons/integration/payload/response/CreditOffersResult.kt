package ai.sovereignrag.commons.integration.payload.response

import javax.money.MonetaryAmount

data class CreditOffersResult(val offerId: Int, val name: String, val amount: MonetaryAmount, val fee: MonetaryAmount)