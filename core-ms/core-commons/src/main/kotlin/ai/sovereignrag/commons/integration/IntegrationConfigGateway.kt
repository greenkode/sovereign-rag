package ai.sovereignrag.commons.integration

import ai.sovereignrag.commons.integration.model.IntegrationConfigAction

interface IntegrationConfigGateway {

    fun getIntegrationConfigId(action: IntegrationConfigAction, identifier: String) : String?
}