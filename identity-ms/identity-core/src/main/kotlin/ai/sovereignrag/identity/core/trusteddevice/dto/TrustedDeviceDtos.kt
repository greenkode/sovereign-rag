package ai.sovereignrag.identity.core.trusteddevice.dto

import an.awesome.pipelinr.Command
import java.time.Instant
import java.util.UUID

data class CheckTrustedDeviceQuery(
    val userId: UUID,
    val deviceFingerprint: String
) : Command<CheckTrustedDeviceResult>

data class CheckTrustedDeviceResult(
    val isTrusted: Boolean,
    val deviceId: UUID? = null,
    val lastUsedAt: Instant? = null,
    val expiresAt: Instant? = null
)

data class TrustDeviceCommand(
    val userId: UUID,
    val sessionId: String,
    val deviceFingerprint: String,
    val deviceName: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val trustDurationDays: Int = 30
) : Command<TrustDeviceResult>

data class TrustDeviceResult(
    val deviceId: UUID,
    val expiresAt: Instant,
    val message: String
)

data class RevokeTrustedDeviceCommand(
    val userId: UUID,
    val deviceId: UUID? = null
) : Command<RevokeTrustedDeviceResult>

data class RevokeTrustedDeviceResult(
    val devicesRevoked: Int,
    val message: String
)

data class GetTrustedDevicesQuery(
    val userId: UUID
) : Command<GetTrustedDevicesResult>

data class GetTrustedDevicesResult(
    val devices: List<TrustedDeviceDto>
)

data class TrustedDeviceDto(
    val id: UUID,
    val deviceName: String?,
    val lastUsedAt: Instant,
    val trustedAt: Instant,
    val expiresAt: Instant,
    val ipAddress: String?,
    val trustCount: Int
)