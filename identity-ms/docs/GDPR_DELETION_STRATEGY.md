# GDPR-Compliant Deletion Strategy

This document describes the GDPR-compliant deletion strategy implemented for users and organizations in the Sovereign RAG Identity Service.

## Overview

The deletion strategy implements a **soft delete with PII masking** approach rather than hard deletion. This ensures:

- GDPR compliance (right to be forgotten)
- Audit trail preservation for compliance requirements
- Referential integrity maintained across the system
- Analytics and operational data preserved (non-PII)

## API Endpoints

All deletion endpoints require `MERCHANT_SUPER_ADMIN` role.

### Delete User
```
DELETE /merchant/deletion/user
Content-Type: application/json

{
  "userId": "uuid-string"
}
```

### Delete Organization
```
DELETE /merchant/deletion/organization
Content-Type: application/json

{
  "organizationId": "uuid-string"
}
```

## User Deletion Process

When a user is deleted, the following operations are performed in order:

### 1. Revoke All Refresh Tokens
All active refresh tokens for the user are revoked by setting `revokedAt` timestamp. This immediately invalidates all active sessions.

### 2. Delete Trusted Devices
All trusted device records are permanently deleted. These contain device fingerprints, IP addresses, and user agents which are considered quasi-PII.

### 3. Anonymize OAuth Provider Accounts
OAuth provider accounts (Google, Microsoft) are anonymized:
- `providerEmail` → `deleted@anonymized.local`
- `providerUserId` → `anonymized_{user_id_prefix}`

### 4. Anonymize and Disable User Account

**PII Fields Anonymized:**
| Field | Anonymized Value |
|-------|------------------|
| `username` | `deleted_user_{id_prefix}` |
| `email` | `deleted_user_{id_prefix}@deleted.local` |
| `password` | Empty string |
| `firstName` | `null` |
| `middleName` | `null` |
| `lastName` | `null` |
| `phoneNumber` | `null` |
| `pictureUrl` | `null` |
| `dateOfBirth` | `null` |
| `taxIdentificationNumber` | `null` |

**Account Status Changes:**
| Field | Value |
|-------|-------|
| `enabled` | `false` |
| `accountNonExpired` | `false` |
| `accountNonLocked` | `false` |
| `credentialsNonExpired` | `false` |
| `authorities` | Cleared |

**Preserved Fields (Non-PII):**
- `id` - Required for audit trail references
- `organizationId` - Required for organizational analytics
- `merchantId` - Required for merchant analytics
- `userType` - Analytics (INDIVIDUAL/BUSINESS)
- `trustLevel` - Analytics
- `registrationSource` - Analytics
- `registrationComplete` - Analytics
- `createdAt` - Audit trail
- `createdBy` - Audit trail
- `lastModifiedAt` - Audit trail
- `lastModifiedBy` - Audit trail
- `version` - Optimistic locking

## Organization Deletion Process

When an organization is deleted, the following operations are performed:

### 1. Delete All Organization Users
All users belonging to the organization are deleted using the user deletion process described above. This ensures all user PII is properly anonymized.

### 2. Soft Delete Registered OAuth Clients
All OAuth registered clients for the organization are soft-deleted:
- `status` → `DELETED`
- `clientName` → `deleted_client_{id_prefix}`
- `domain` → `null`

### 3. Anonymize Organization

**PII Fields Anonymized:**
| Field | Anonymized Value |
|-------|------------------|
| `name` | `deleted_org_{id_prefix}` |
| `slug` | `deleted_org_{id_prefix}` |
| `settings` | Empty map |

**Status Change:**
- `status` → `DELETED`

**Preserved Fields (Non-PII):**
- `id` - Required for audit trail references
- `plan` - Analytics (subscription tier)
- `databaseName` - Infrastructure reference
- `databaseCreated` - Infrastructure state
- `maxKnowledgeBases` - Configuration
- `createdAt` - Audit trail
- `createdBy` - Audit trail
- `lastModifiedAt` - Audit trail
- `lastModifiedBy` - Audit trail
- `version` - Optimistic locking

## Data Retention

### Immediately Deleted (Hard Delete)
- Trusted devices
- (Provider accounts are anonymized, not deleted, to maintain referential integrity)

### Anonymized (Soft Delete)
- User accounts
- OAuth provider accounts
- Organizations
- OAuth registered clients

### Preserved Indefinitely
- Audit logs (in audit-ms)
- Non-PII operational metadata

## Implementation Details

### Services
- `UserDeletionService` - Handles user deletion workflow
- `OrganizationDeletionService` - Handles organization deletion workflow

### Repository Methods
- `RefreshTokenRepository.revokeAllUserTokens()` - Revokes tokens via `@Modifying` query
- `TrustedDeviceRepository.deleteAllByUserId()` - Hard deletes devices
- `OAuthProviderAccountRepository.anonymizeByUserId()` - Anonymizes via `@Modifying` query
- `OAuthUserRepository.findByOrganizationId()` - Finds users for bulk deletion
- `OAuthRegisteredClientRepository.findByOrganizationId()` - Finds clients for soft delete

### Transaction Handling
All deletion operations are wrapped in a single transaction (`@Transactional`) to ensure atomicity. If any step fails, the entire operation is rolled back.

## Security Considerations

1. **Authorization**: Only users with `MERCHANT_SUPER_ADMIN` role can perform deletions
2. **Audit Trail**: All deletion operations are logged
3. **Idempotency**: Deleting an already-deleted organization returns an error
4. **Cascade Safety**: User deletion is performed before organization anonymization to ensure proper cleanup

## Response Format

### Success Response
```json
{
  "success": true,
  "message": "user.deleted_successfully",
  "userId": "uuid-string"
}
```

### Organization Deletion Success
```json
{
  "success": true,
  "message": "organization.deleted_successfully",
  "organizationId": "uuid-string",
  "usersDeleted": 5
}
```

### Error Responses
```json
{
  "success": false,
  "message": "error.user_not_found",
  "userId": "uuid-string"
}
```

```json
{
  "success": false,
  "message": "error.organization_already_deleted",
  "organizationId": "uuid-string",
  "usersDeleted": 0
}
```

## Future Considerations

1. **Data Export**: Implement GDPR data portability (right to data export) before deletion
2. **Scheduled Cleanup**: Add scheduled job to permanently delete anonymized records after retention period
3. **Knowledge Base Cleanup**: Coordinate with core-ms to clean up organization's knowledge bases
4. **Notification**: Send confirmation email before/after deletion
5. **Grace Period**: Implement soft-delete grace period for accidental deletion recovery
