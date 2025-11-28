package ai.sovereignrag.identity.core.settings.dto

import ai.sovereignrag.identity.core.entity.CompanyRole
import ai.sovereignrag.identity.core.entity.CompanySize
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.entity.IntendedPurpose
import ai.sovereignrag.identity.core.entity.OrganizationPlan
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User settings response")
data class UserSettingsResponse(
    @Schema(description = "User first name", example = "John")
    val firstName: String?,
    @Schema(description = "User last name", example = "Doe")
    val lastName: String?,
    @Schema(description = "User email address", example = "john.doe@example.com")
    val email: String,
    @Schema(description = "List of user roles", example = "[\"MERCHANT_ADMIN\"]")
    val roles: List<String>,
    @Schema(description = "Merchant name", example = "ABC Company Ltd")
    val merchantName: String?
)

@Schema(description = "Update user name request")
data class UpdateUserNameRequest(
    @Schema(description = "New first name", example = "John")
    val firstName: String?,
    @Schema(description = "New last name", example = "Doe")
    val lastName: String?
)

@Schema(description = "Update user name response")
data class UpdateUserNameResponse(
    @Schema(description = "Whether update was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "User name updated successfully")
    val message: String
)

@Schema(description = "Reset keys request")
data class ResetKeysRequest(
    @Schema(description = "Environment to reset keys for (SANDBOX or PRODUCTION)", example = "SANDBOX")
    val environment: EnvironmentMode
)

@Schema(description = "Reset keys response")
data class ResetKeysResponse(
    @Schema(description = "Client ID", example = "client_abc123")
    val clientId: String,
    @Schema(description = "New client secret", example = "secret_xyz789")
    val clientSecret: String,
    @Schema(description = "Environment that was reset", example = "SANDBOX")
    val environment: EnvironmentMode,
    @Schema(description = "Whether reset was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Keys reset successfully")
    val message: String
)

@Schema(description = "Update alerts request")
data class UpdateAlertsRequest(
    @Schema(description = "Transaction failure rate limit percentage", example = "5.0", required = true)
    val failureLimit: java.math.BigDecimal,
    @Schema(description = "Low balance alert threshold", example = "10000.00", required = true)
    val lowBalance: java.math.BigDecimal
)

@Schema(description = "Update alerts response")
data class UpdateAlertsResponse(
    @Schema(description = "Whether update was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Alert settings updated successfully")
    val message: String
)

@Schema(description = "Enable production request")
data class EnableProductionRequest(
    @Schema(description = "Environment mode to enable (PRODUCTION or SANDBOX)", example = "PRODUCTION")
    val environmentMode: EnvironmentMode
)

@Schema(description = "Enable production response")
data class EnableProductionResponse(
    @Schema(description = "Merchant ID", example = "abc123")
    val merchantId: String,
    @Schema(description = "Environment mode enabled", example = "PRODUCTION")
    val environmentMode: EnvironmentMode,
    @Schema(description = "Number of users affected by the change", example = "5")
    val affectedUsers: Int,
    @Schema(description = "Whether update was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Merchant enabled for production")
    val message: String
)

@Schema(description = "Complete organization setup request")
data class CompleteOrganizationSetupRequest(
    @Schema(description = "Company name", example = "Acme Corporation", required = true)
    val companyName: String,
    @Schema(description = "Intended purpose for using the platform", example = "CUSTOMER_SUPPORT", required = true)
    val intendedPurpose: IntendedPurpose,
    @Schema(description = "Company size", example = "SIZE_11_50", required = true)
    val companySize: CompanySize,
    @Schema(description = "User's role in the company", example = "CTO", required = true)
    val roleInCompany: CompanyRole,
    @Schema(description = "Country ISO2 code", example = "US", required = true)
    val country: String,
    @Schema(description = "Phone number", example = "+1234567890", required = true)
    val phoneNumber: String,
    @Schema(description = "Whether terms and conditions are accepted", example = "true", required = true)
    val termsAccepted: Boolean
)

@Schema(description = "Complete organization setup response")
data class CompleteOrganizationSetupResponse(
    @Schema(description = "Whether setup was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Organization setup completed successfully")
    val message: String,
    @Schema(description = "Merchant ID", example = "abc123")
    val merchantId: String? = null
)

@Schema(description = "Organization details response")
data class OrganizationDetailsResponse(
    @Schema(description = "Organization ID", example = "abc123")
    val id: String,
    @Schema(description = "Organization name", example = "Acme Corporation")
    val name: String,
    @Schema(description = "Organization subscription plan", example = "PROFESSIONAL")
    val plan: OrganizationPlan,
    @Schema(description = "Organization status", example = "ACTIVE")
    val status: OrganizationStatus,
    @Schema(description = "Current environment mode", example = "SANDBOX")
    val environmentMode: EnvironmentMode,
    @Schema(description = "Whether organization setup is completed", example = "true")
    val setupCompleted: Boolean
)