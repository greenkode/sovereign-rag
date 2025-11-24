package ai.sovereignrag.commons.integration

import ai.sovereignrag.commons.integration.payload.response.CreditRatingCheckResult
import java.util.UUID

interface CreditRatingGateway {

    fun getCreditRating(vendorId: String, accountNumber: String): CreditRatingCheckResult
}