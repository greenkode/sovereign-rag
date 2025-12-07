package ai.sovereignrag.commons.regional

import javax.sql.DataSource

interface RegionalDataSourceRegistry {
    fun getDataSource(regionCode: String): DataSource
    fun getReadReplicaDataSource(regionCode: String): DataSource
    fun getAvailableRegions(): Set<String>
    fun isRegionAvailable(regionCode: String): Boolean
    fun getDatabaseUrl(regionCode: String): String
}

class RegionNotConfiguredException(regionCode: String) :
    RuntimeException("Region '$regionCode' is not configured. Available regions: check regional-databases configuration")

class RegionNotAvailableException(regionCode: String, availableRegions: Set<String>) :
    RuntimeException("Region '$regionCode' is not available. Available regions: $availableRegions")
