package ai.sovereignrag.accounting.minigl.common

import ai.sovereignrag.commons.accounting.TransactionGroup
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.currency.DefaultCurrency

object MiniglConstants {

    const val DEFAULT_CHART_NAME = DefaultCurrency.DEFAULT_CURRENCY_CODE
    
    const val BRIDGE_LIABILITIES = "bridge-liabilities"
    const val BRIDGE_ASSETS = "bridge-assets"
    
    val DEFAULT_TRANSACTION_GROUP = TransactionGroup.BILL_PAYMENT
    
    // Context-based transaction group mappings
    val BILL_PAYMENT_GROUP = TransactionGroup.BILL_PAYMENT
    val P2P_GROUP = TransactionGroup.P2P
    val INBOUND_GROUP = TransactionGroup.INBOUND
    val OUTBOUND_GROUP = TransactionGroup.OUTBOUND
    
    // Entry type constants for metadata checks
    object EntryTypes {
        val AMOUNT = EntryType.AMOUNT.name
        val FEE = EntryType.FEE.name
        val VAT = EntryType.VAT.name
        val COMMISSION = EntryType.COMMISSION.name
        val REBATE = EntryType.REBATE.name
    }
}