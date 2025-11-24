package ai.sovereignrag.commons.integration.payload.response

data class CreditRatingCheckResult(val qualifies: Boolean, val offers: List<CreditOffersResult>, val metadata: Map<String, String>)