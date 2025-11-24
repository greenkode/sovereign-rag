package ai.sovereignrag.license.repository

import ai.sovereignrag.license.domain.License
import ai.sovereignrag.license.domain.LicenseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface LicenseRepository : JpaRepository<License, UUID> {

    fun findByLicenseKey(licenseKey: String): License?

    fun findByClientId(clientId: String): List<License>

    fun findByStatus(status: LicenseStatus): List<License>

    @Query("SELECT l FROM License l WHERE l.expiresAt < :now AND l.status = 'ACTIVE'")
    fun findExpiredLicenses(now: Instant = Instant.now()): List<License>

    @Query("SELECT l FROM License l WHERE l.expiresAt > :now AND l.expiresAt < :warningDate AND l.status = 'ACTIVE'")
    fun findExpiringLicenses(now: Instant = Instant.now(), warningDate: Instant): List<License>

    fun findByClientIdAndStatus(clientId: String, status: LicenseStatus): List<License>
}
