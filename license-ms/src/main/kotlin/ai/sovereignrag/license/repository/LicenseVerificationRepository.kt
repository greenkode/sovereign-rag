package ai.sovereignrag.license.repository

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Repository
class LicenseVerificationRepository(
    private val entityManager: EntityManager
) {

    @Transactional
    fun recordVerification(
        licenseKeyHash: String,
        clientId: String,
        deploymentId: String?,
        ipAddress: String?,
        hostname: String?,
        applicationVersion: String?,
        success: Boolean,
        failureReason: String?,
        metadata: Map<String, Any>?
    ) {
        val sql = """
            INSERT INTO license.license_verification
            (license_key_hash, customer_id, deployment_id, ip_address, hostname,
             application_version, verification_time, success, failure_reason, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        """.trimIndent()

        entityManager.createNativeQuery(sql)
            .setParameter(1, licenseKeyHash)
            .setParameter(2, clientId)
            .setParameter(3, deploymentId)
            .setParameter(4, ipAddress)
            .setParameter(5, hostname)
            .setParameter(6, applicationVersion)
            .setParameter(7, Instant.now())
            .setParameter(8, success)
            .setParameter(9, failureReason)
            .setParameter(10, metadata?.let { mapToJson(it) })
            .executeUpdate()
    }

    @Transactional
    fun recordUsage(
        licenseKeyHash: String,
        clientId: String,
        deploymentId: String?,
        tokensUsed: Long,
        activeTenants: Int,
        activeUsers: Int,
        apiCalls: Long,
        metadata: Map<String, Any>?
    ) {
        val sql = """
            INSERT INTO license.license_usage
            (license_key_hash, customer_id, deployment_id, report_date,
             tokens_used, active_tenants, active_users, api_calls, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (license_key_hash, deployment_id, report_date)
            DO UPDATE SET
                tokens_used = license.license_usage.tokens_used + EXCLUDED.tokens_used,
                active_tenants = GREATEST(license.license_usage.active_tenants, EXCLUDED.active_tenants),
                active_users = GREATEST(license.license_usage.active_users, EXCLUDED.active_users),
                api_calls = license.license_usage.api_calls + EXCLUDED.api_calls,
                metadata = EXCLUDED.metadata
        """.trimIndent()

        entityManager.createNativeQuery(sql)
            .setParameter(1, licenseKeyHash)
            .setParameter(2, clientId)
            .setParameter(3, deploymentId)
            .setParameter(4, LocalDate.now())
            .setParameter(5, tokensUsed)
            .setParameter(6, activeTenants)
            .setParameter(7, activeUsers)
            .setParameter(8, apiCalls)
            .setParameter(9, metadata?.let { mapToJson(it) })
            .executeUpdate()
    }

    private fun mapToJson(map: Map<String, Any>): String {
        return map.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { (key, value) ->
            val jsonValue = when (value) {
                is String -> "\"${value.replace("\"", "\\\"")}\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"$value\""
            }
            "\"$key\":$jsonValue"
        }
    }
}
