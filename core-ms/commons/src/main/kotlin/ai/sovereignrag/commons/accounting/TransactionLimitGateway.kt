package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.accounting.dto.SubscriptionLimitDto
import java.util.UUID

interface SubscriptionLimitGateway {

    fun findByTenant(tenantId: UUID): SubscriptionLimitDto?
}