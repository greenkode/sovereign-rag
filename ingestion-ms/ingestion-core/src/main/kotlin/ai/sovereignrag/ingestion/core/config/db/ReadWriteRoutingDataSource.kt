package ai.sovereignrag.ingestion.core.config.db

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

class ReadWriteRoutingDataSource : AbstractRoutingDataSource() {

    companion object {
        const val WRITE_DATASOURCE = "write"
        const val READ_DATASOURCE = "read"
    }

    override fun determineCurrentLookupKey(): Any {
        return if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            READ_DATASOURCE
        } else {
            WRITE_DATASOURCE
        }
    }
}
