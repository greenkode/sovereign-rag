package ai.sovereignrag.accounting.minigl.transaction.service.bridge

import ai.sovereignrag.accounting.minigl.account.dao.MiniglAccountRepository
import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import org.springframework.stereotype.Component
import ai.sovereignrag.accounting.entity.CompositeAccountEntity as CompositeAccount
import ai.sovereignrag.accounting.entity.FinalAccountEntity as FinalAccount

interface BridgeAccountResolver {
    fun resolveAssetBridge(account: FinalAccount, chart: CompositeAccount): FinalAccount?
    fun resolveLiabilityBridge(account: FinalAccount, chart: CompositeAccount): FinalAccount?
    fun getAllBridgeAccounts(accounts: Collection<FinalAccount>, chart: CompositeAccount): Map<String, FinalAccount>
}

@Component
class DefaultBridgeAccountResolver(
    private val miniglAccountRepository: MiniglAccountRepository
) : BridgeAccountResolver {
    
    override fun resolveAssetBridge(account: FinalAccount, chart: CompositeAccount): FinalAccount? {
        val bridgeDescription = "${MiniglConstants.BRIDGE_ASSETS}-${account.description}"
        return miniglAccountRepository.getFinalAccountsByChartAndDescriptionIn(chart, setOf(bridgeDescription))
            .firstOrNull { it.description == bridgeDescription }
    }
    
    override fun resolveLiabilityBridge(account: FinalAccount, chart: CompositeAccount): FinalAccount? {
        val bridgeDescription = "${MiniglConstants.BRIDGE_LIABILITIES}-${account.description}"
        return miniglAccountRepository.getFinalAccountsByChartAndDescriptionIn(chart, setOf(bridgeDescription))
            .firstOrNull { it.description == bridgeDescription }
    }
    
    override fun getAllBridgeAccounts(accounts: Collection<FinalAccount>, chart: CompositeAccount): Map<String, FinalAccount> {
        val bridgeDescriptions = accounts.flatMap { account ->
            listOf(
                "${MiniglConstants.BRIDGE_LIABILITIES}-${account.description}",
                "${MiniglConstants.BRIDGE_ASSETS}-${account.description}"
            )
        }.toSet()
        
        return miniglAccountRepository.getFinalAccountsByChartAndDescriptionIn(chart, bridgeDescriptions)
            .associateBy { it.description }
    }
}