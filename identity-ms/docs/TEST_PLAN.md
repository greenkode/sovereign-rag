# Identity-MS Comprehensive Test Plan

## Executive Summary

This document outlines the complete testing strategy for the identity-ms microservice, covering unit tests, integration tests, and end-to-end acceptance tests using Playwright. The plan is organized into phases with clear task breakdowns and effort estimates.

---

## Current State Analysis

### Existing Test Coverage
| Category | Files | Status |
|----------|-------|--------|
| Unit Tests (Command Handlers) | 5 | Partial |
| Integration Tests | 0 | Base class only |
| E2E Tests (Playwright) | 2 | Minimal |
| Service Tests | 0 | Missing |
| Controller Tests | 0 | Missing |

### Test Infrastructure Available
- ✅ MockK for mocking
- ✅ JUnit 5 + Kotlin Test
- ✅ TestContainers with PostgreSQL
- ✅ Playwright (frontend)
- ✅ Spring Boot Test
- ❌ Code coverage (JaCoCo) - Not configured
- ❌ Test data builders - Not implemented

---

## Phase 1: Infrastructure Setup

### 1.1 Test Data Builders & Fixtures
**Effort: 4-6 hours**

Create reusable test data factories for consistent test data across all test types.

**Tasks:**
- [ ] Create `TestDataBuilders.kt` with builders for:
  - `OAuthUserBuilder` - Build test users with various states
  - `OAuthRegisteredClientBuilder` - Build test organizations
  - `ProcessDtoBuilder` - Build mock process responses
  - `TokenBuilder` - Build JWT tokens for testing
- [ ] Create `TestFixtures.kt` with predefined scenarios:
  - Verified user, unverified user, locked user
  - Active organization, pending organization
  - Valid/expired/invalid tokens

**Files to create:**
```
identity-core/src/test/kotlin/ai/sovereignrag/identity/core/testutil/
├── TestDataBuilders.kt
├── TestFixtures.kt
└── TestExtensions.kt
```

### 1.2 Integration Test Base Enhancement
**Effort: 3-4 hours**

Enhance the existing `AbstractIntegrationTest` with:
- [ ] Database cleanup between tests
- [ ] Test data seeding utilities
- [ ] Transaction management
- [ ] Mock external services (ProcessGateway, NotificationClient)

**Files to modify/create:**
```
identity-core/src/test/kotlin/ai/sovereignrag/identity/core/integration/
├── AbstractIntegrationTest.kt (enhance)
├── MockExternalServices.kt (new)
└── DatabaseCleanup.kt (new)
```

### 1.3 Playwright E2E Test Setup Enhancement
**Effort: 2-3 hours**

- [ ] Create test user seeding script
- [ ] Configure test environment variables
- [ ] Setup test database with known state
- [ ] Create page object models for auth pages

**Files to create:**
```
sovereign-rag-client/e2e/
├── setup/
│   ├── seed-test-data.ts
│   └── test-env.ts
├── page-objects/
│   ├── login.page.ts
│   ├── register.page.ts
│   ├── verify-email.page.ts
│   ├── forgot-password.page.ts
│   └── reset-password.page.ts
└── fixtures/
    └── test-users.ts
```

---

## Phase 2: Unit Tests

### 2.1 Authentication Command Handlers
**Effort: 8-10 hours**

#### LoginCommandHandler Tests
**File:** `LoginCommandHandlerTest.kt`
**Effort: 2 hours**

| Test Case | Priority |
|-----------|----------|
| Should login successfully with valid credentials | High |
| Should return tokens with correct expiry | High |
| Should throw exception for invalid password | High |
| Should throw exception for non-existent user | High |
| Should throw exception for locked account | High |
| Should increment failed login attempts on failure | High |
| Should reset failed attempts on success | Medium |
| Should publish audit event on success | Medium |
| Should publish audit event on failure | Medium |
| Should handle case-insensitive username | Low |

#### InitiateTwoFactorCommandHandler Tests
**File:** `InitiateTwoFactorCommandHandlerTest.kt`
**Effort: 2-3 hours**

| Test Case | Priority |
|-----------|----------|
| Should initiate 2FA and send code | High |
| Should skip 2FA for trusted device | High |
| Should create 2FA session process | High |
| Should throw for invalid credentials | High |
| Should throw for unverified email | High |
| Should throw for locked account | Medium |
| Should generate device fingerprint | Medium |
| Should send email with correct code format | Medium |

#### VerifyTwoFactorCommandHandler Tests
**File:** `VerifyTwoFactorCommandHandlerTest.kt`
**Effort: 1-2 hours**

| Test Case | Priority |
|-----------|----------|
| Should verify correct code and return tokens | High |
| Should throw for invalid code | High |
| Should throw for expired session | High |
| Should mark device as trusted when requested | Medium |
| Should complete 2FA process | Medium |

#### RefreshTokenCommandHandler Tests
**File:** `RefreshTokenCommandHandlerTest.kt`
**Effort: 1-2 hours**

| Test Case | Priority |
|-----------|----------|
| Should refresh token successfully | High |
| Should throw for invalid refresh token | High |
| Should throw for revoked token | High |
| Should throw for mismatched JTI | High |
| Should return new access token | Medium |

#### LogoutCommandHandler Tests
**File:** `LogoutCommandHandlerTest.kt`
**Effort: 0.5 hours**

| Test Case | Priority |
|-----------|----------|
| Should revoke refresh token | High |
| Should return success even if token not found | Medium |

### 2.2 Password Reset Command Handlers
**Effort: 4-5 hours**

#### InitiatePasswordResetCommandHandler Tests
**File:** `InitiatePasswordResetCommandHandlerTest.kt`
**Effort: 1.5-2 hours**

| Test Case | Priority |
|-----------|----------|
| Should create password reset process | High |
| Should send reset email | High |
| Should prevent duplicate requests within 5 minutes | High |
| Should throw for non-existent user | High |
| Should generate secure token | Medium |
| Should include correct reset link | Medium |

#### ValidatePasswordResetCommandHandler Tests
**File:** `ValidatePasswordResetCommandHandlerTest.kt`
**Effort: 1 hour**

| Test Case | Priority |
|-----------|----------|
| Should validate correct token | High |
| Should throw for invalid token | High |
| Should throw for expired token | High |
| Should return user reference | Medium |

#### CompletePasswordResetCommandHandler Tests
**File:** `CompletePasswordResetCommandHandlerTest.kt`
**Effort: 1.5-2 hours**

| Test Case | Priority |
|-----------|----------|
| Should update password successfully | High |
| Should complete reset process | High |
| Should throw for invalid token | High |
| Should throw for mismatched reference | High |
| Should encode new password | Medium |

### 2.3 Service Unit Tests
**Effort: 6-8 hours**

#### JwtTokenService Tests
**File:** `JwtTokenServiceTest.kt`
**Effort: 2 hours**

| Test Case | Priority |
|-----------|----------|
| Should generate valid JWT token | High |
| Should include correct claims | High |
| Should set correct expiry | High |
| Should generate refresh token | High |
| Should include user authorities | Medium |
| Should include merchant ID | Medium |

#### AccountLockoutService Tests
**File:** `AccountLockoutServiceTest.kt`
**Effort: 1.5 hours**

| Test Case | Priority |
|-----------|----------|
| Should record failed login | High |
| Should lock account after max attempts | High |
| Should reset on successful login | High |
| Should auto-unlock after duration | High |
| Should calculate remaining lockout time | Medium |

#### BusinessEmailValidationService Tests
**File:** `BusinessEmailValidationServiceTest.kt`
**Effort: 1 hour**

| Test Case | Priority |
|-----------|----------|
| Should accept valid business email | High |
| Should reject personal email domains | High |
| Should reject invalid email format | High |
| Should be case insensitive | Medium |
| Should handle all blocked domains | Medium |

#### RefreshTokenService Tests
**File:** `RefreshTokenServiceTest.kt`
**Effort: 1.5 hours**

| Test Case | Priority |
|-----------|----------|
| Should create and store token hash | High |
| Should validate token and JTI | High |
| Should revoke token | High |
| Should reject expired tokens | High |
| Should track device fingerprint | Medium |

#### CustomUserDetailsService Tests
**File:** `CustomUserDetailsServiceTest.kt`
**Effort: 1 hour**

| Test Case | Priority |
|-----------|----------|
| Should load user by username | High |
| Should throw for non-existent user | High |
| Should check account lock status | High |
| Should return correct authorities | Medium |

---

## Phase 3: Integration Tests

### 3.1 Repository Integration Tests
**Effort: 4-5 hours**

#### OAuthUserRepository Tests
**File:** `OAuthUserRepositoryIntegrationTest.kt`
**Effort: 1.5 hours**

| Test Case | Priority |
|-----------|----------|
| Should save and find user by email | High |
| Should update user fields | High |
| Should find super admins by merchant | Medium |
| Should handle authorities collection | Medium |

#### OAuthRegisteredClientRepository Tests
**File:** `OAuthRegisteredClientRepositoryIntegrationTest.kt`
**Effort: 1.5 hours**

| Test Case | Priority |
|-----------|----------|
| Should save and find client by domain | High |
| Should save and find client by ID | High |
| Should handle scopes, methods, grants | Medium |
| Should handle settings and token settings | Medium |

#### RefreshTokenRepository Tests
**File:** `RefreshTokenRepositoryIntegrationTest.kt`
**Effort: 1 hour**

| Test Case | Priority |
|-----------|----------|
| Should save and find by JTI | High |
| Should find by token hash | High |
| Should delete expired tokens | Medium |

### 3.2 Service Integration Tests
**Effort: 6-8 hours**

#### Registration Flow Integration Tests
**File:** `RegistrationIntegrationTest.kt`
**Effort: 2-3 hours**

| Test Case | Priority |
|-----------|----------|
| Should register new user with new organization | High |
| Should register user in existing organization | High |
| Should allow re-registration for incomplete registration | High |
| Should reject duplicate complete registration | High |
| Should create OAuth client with correct settings | Medium |
| Should assign admin roles to first user | Medium |

#### Authentication Flow Integration Tests
**File:** `AuthenticationIntegrationTest.kt`
**Effort: 2-3 hours**

| Test Case | Priority |
|-----------|----------|
| Should authenticate valid user | High |
| Should reject invalid credentials | High |
| Should handle account lockout | High |
| Should generate and validate tokens | High |
| Should refresh tokens correctly | Medium |

#### Password Reset Flow Integration Tests
**File:** `PasswordResetIntegrationTest.kt`
**Effort: 2 hours**

| Test Case | Priority |
|-----------|----------|
| Should complete full password reset flow | High |
| Should reject expired reset tokens | High |
| Should prevent duplicate reset requests | Medium |

### 3.3 Controller Integration Tests
**Effort: 8-10 hours**

#### RegistrationController Tests
**File:** `RegistrationControllerIntegrationTest.kt`
**Effort: 2-3 hours**

| Test Case | Priority |
|-----------|----------|
| POST /api/registration/register - success | High |
| POST /api/registration/register - duplicate email | High |
| POST /api/registration/register - personal email rejected | High |
| POST /api/registration/verify-email - valid token | High |
| POST /api/registration/verify-email - invalid token | High |
| POST /api/registration/resend-verification - success | Medium |
| Rate limiting validation | Medium |

#### LoginController Tests
**File:** `LoginControllerIntegrationTest.kt`
**Effort: 2 hours**

| Test Case | Priority |
|-----------|----------|
| POST /api/login - success | High |
| POST /api/login - invalid credentials | High |
| POST /api/login - locked account | High |
| Rate limiting validation | Medium |

#### TwoFactorAuthController Tests
**File:** `TwoFactorAuthControllerIntegrationTest.kt`
**Effort: 2-3 hours**

| Test Case | Priority |
|-----------|----------|
| POST /api/2fa/login - initiates 2FA | High |
| POST /api/2fa/verify - correct code | High |
| POST /api/2fa/verify - incorrect code | High |
| POST /api/2fa/resend - resends code | Medium |
| POST /api/2fa/refresh - refreshes token | High |
| POST /api/2fa/logout - revokes token | Medium |

#### PasswordResetController Tests
**File:** `PasswordResetControllerIntegrationTest.kt`
**Effort: 2 hours**

| Test Case | Priority |
|-----------|----------|
| POST /password-reset/initiate - success | High |
| POST /password-reset/validate - valid token | High |
| POST /password-reset/validate - invalid token | High |
| POST /password-reset/complete - success | High |
| Rate limiting validation | Medium |

---

## Phase 4: End-to-End Tests (Playwright)

### 4.1 Registration Flow E2E
**Effort: 4-5 hours**

**File:** `e2e/tests/registration.spec.ts`

| Test Case | Priority |
|-----------|----------|
| User can register with valid business email | High |
| Registration shows validation errors for invalid input | High |
| Personal email domains are rejected | High |
| User receives verification email (mock check) | Medium |
| Email verification link works | High |
| Expired verification link shows error | Medium |
| Resend verification works | Medium |
| OAuth registration with Google works | High |
| OAuth registration with Microsoft works | High |

### 4.2 Login Flow E2E
**Effort: 3-4 hours**

**File:** `e2e/tests/login.spec.ts`

| Test Case | Priority |
|-----------|----------|
| User can login with valid credentials | High |
| Invalid credentials show error | High |
| Account lockout after failed attempts | High |
| Locked account shows appropriate message | Medium |
| Remember me functionality | Low |
| OAuth login with Google works | High |
| OAuth login with Microsoft works | High |
| Redirect to dashboard after login | High |
| Redirect to org setup if incomplete | High |

### 4.3 Password Reset Flow E2E
**Effort: 3-4 hours**

**File:** `e2e/tests/password-reset.spec.ts`

| Test Case | Priority |
|-----------|----------|
| User can initiate password reset | High |
| Reset email is sent (mock check) | Medium |
| Valid reset link shows form | High |
| Invalid/expired link shows error | High |
| Password validation works | High |
| Password mismatch shows error | Medium |
| Successful reset allows login | High |

### 4.4 Session Management E2E
**Effort: 2-3 hours**

**File:** `e2e/tests/session.spec.ts`

| Test Case | Priority |
|-----------|----------|
| Token refresh works seamlessly | High |
| Logout clears session | High |
| Protected routes redirect to login | High |
| Session persists across page refresh | Medium |

### 4.5 Two-Factor Authentication E2E
**Effort: 3-4 hours**

**File:** `e2e/tests/two-factor.spec.ts`

| Test Case | Priority |
|-----------|----------|
| 2FA is triggered for new device | High |
| Correct code allows login | High |
| Incorrect code shows error | High |
| Resend code works | Medium |
| Trust device skips future 2FA | Medium |

---

## Effort Summary

### By Phase

| Phase | Category | Estimated Hours |
|-------|----------|-----------------|
| Phase 1 | Infrastructure Setup | 9-13 |
| Phase 2 | Unit Tests | 18-23 |
| Phase 3 | Integration Tests | 18-23 |
| Phase 4 | E2E Tests (Playwright) | 15-20 |
| **Total** | | **60-79 hours** |

### By Priority

| Priority | Test Count | Estimated Hours |
|----------|------------|-----------------|
| High | ~70 tests | 40-50 |
| Medium | ~40 tests | 15-20 |
| Low | ~5 tests | 2-3 |

---

## Implementation Order (Recommended)

### Sprint 1: Foundation (Week 1)
1. Test Data Builders & Fixtures
2. Integration Test Base Enhancement
3. Unit Tests for existing handlers (fill gaps)

### Sprint 2: Core Auth Unit Tests (Week 2)
1. LoginCommandHandler Tests
2. InitiateTwoFactorCommandHandler Tests
3. VerifyTwoFactorCommandHandler Tests
4. RefreshTokenCommandHandler Tests
5. LogoutCommandHandler Tests

### Sprint 3: Password & Service Tests (Week 3)
1. Password Reset Handler Tests (all 3)
2. JwtTokenService Tests
3. AccountLockoutService Tests
4. BusinessEmailValidationService Tests
5. RefreshTokenService Tests

### Sprint 4: Integration Tests (Week 4)
1. Repository Integration Tests
2. Registration Flow Integration Tests
3. Authentication Flow Integration Tests
4. Password Reset Flow Integration Tests

### Sprint 5: Controller & E2E Tests (Week 5)
1. Controller Integration Tests
2. Playwright E2E Setup Enhancement
3. Registration E2E Tests
4. Login E2E Tests

### Sprint 6: Remaining E2E & Polish (Week 6)
1. Password Reset E2E Tests
2. Session Management E2E Tests
3. 2FA E2E Tests
4. Code coverage setup (JaCoCo)
5. Test documentation

---

## Environment Requirements

### For Integration Tests
- Docker (for TestContainers)
- PostgreSQL 15+ (via TestContainers)
- Java 21+
- Maven 3.9+

### For E2E Tests
- Node.js 18+
- Playwright browsers (auto-installed)
- Running backend services (identity-ms, notification-ms mock)
- Running frontend (npm run dev)
- Test database with seed data

### Environment Variables for Tests
```bash
# Integration Tests
SPRING_PROFILES_ACTIVE=test
TEST_DB_URL=jdbc:tc:postgresql:15-alpine:///identity_test

# E2E Tests
BASE_URL=http://localhost:3000
IDENTITY_MS_URL=http://localhost:9083
TEST_USER_EMAIL=test@example.com
TEST_USER_PASSWORD=TestPassword123!
```

---

## Dependencies to Add

### For Code Coverage (Optional but Recommended)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
</plugin>
```

### For REST API Testing (Optional)
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Success Criteria

1. **Unit Test Coverage**: >80% line coverage for command handlers and services
2. **Integration Test Coverage**: All critical paths tested with real database
3. **E2E Test Coverage**: All user-facing auth flows tested
4. **CI Pipeline**: All tests pass in CI before merge
5. **Test Reliability**: <1% flaky test rate
