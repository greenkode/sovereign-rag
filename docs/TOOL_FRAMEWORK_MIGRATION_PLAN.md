# Tool Framework & Guard System Migration Plan

## Overview
This document outlines the migration plan for implementing a comprehensive tool framework with guard system for the Compilot AI platform.

## Phase 1: Guard System Foundation (Week 1-2)

### 1.1 Core Guard Infrastructure
**Priority**: CRITICAL
**Dependencies**: None

**Tasks**:
- [ ] Create `Guard` interface
- [ ] Create `GuardContext` data class
- [ ] Create `GuardResult` sealed class (Allow, Deny, RequireConfirmation)
- [ ] Create `GuardChain` for composing multiple guards
- [ ] Create `GuardRegistry` for managing guards
- [ ] Create `GuardExecutor` service

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/Guard.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardContext.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardResult.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardChain.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardRegistry.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardExecutor.kt`

### 1.2 Rate Limiting Guards
**Priority**: CRITICAL
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `RateLimitGuard` interface
- [ ] Implement `SessionRateLimitGuard` (per-session limits)
- [ ] Implement `TenantRateLimitGuard` (per-tenant limits)
- [ ] Implement `GlobalRateLimitGuard` (system-wide limits)
- [ ] Create `RateLimitState` storage (Redis-backed)
- [ ] Configure rate limits in application.yml

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/RateLimitGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/SessionRateLimitGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/TenantRateLimitGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/GlobalRateLimitGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/RateLimitState.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/ratelimit/RateLimitConfig.kt`

**Rate Limits to Configure**:
- Email: 5 per session, 20 per user per day, 1 per 30 seconds
- Escalation: 3 per session, 10 per user per day
- Search: 100 per session, 1000 per user per day
- API calls: 10 per minute per tenant

### 1.3 Authorization Guards
**Priority**: CRITICAL
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `Permission` enum
- [ ] Create `AuthorizationGuard` interface
- [ ] Implement `PermissionGuard` (checks tool permissions)
- [ ] Implement `TenantIsolationGuard` (ensures tenant data isolation)
- [ ] Implement `RoleBasedGuard` (checks user roles)
- [ ] Integrate with existing TenantContext

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/Permission.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/auth/AuthorizationGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/auth/PermissionGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/auth/TenantIsolationGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/auth/RoleBasedGuard.kt`

**Permissions to Define**:
- READ (search, retrieve)
- WRITE (create, update)
- COMMUNICATE (email, SMS)
- ESCALATE (escalations)
- ADMIN (delete, modify settings)
- DESTRUCTIVE (purge data)

### 1.4 Input Validation Guards
**Priority**: CRITICAL
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `ValidationGuard` interface
- [ ] Implement `EmailValidationGuard`
- [ ] Implement `PhoneValidationGuard`
- [ ] Implement `InjectionPreventionGuard` (SQL, XSS, command injection)
- [ ] Implement `ProfanityFilterGuard`
- [ ] Create validation utilities

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/validation/ValidationGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/validation/EmailValidationGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/validation/PhoneValidationGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/validation/InjectionPreventionGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/validation/ProfanityFilterGuard.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/validation/ValidationUtils.kt`

### 1.5 Apply Guards to EmailTool
**Priority**: CRITICAL
**Dependencies**: 1.2, 1.3, 1.4

**Tasks**:
- [ ] Refactor EmailTool to use guard system
- [ ] Apply rate limiting guards
- [ ] Apply authorization guards
- [ ] Apply validation guards
- [ ] Add guard violation logging
- [ ] Test EmailTool with guards

**Files to Modify**:
- `core-ai/src/main/kotlin/nl/compilot/ai/tools/EmailTool.kt`

## Phase 2: Content & Behavioral Guards (Week 3)

### 2.1 PII Detection & Redaction
**Priority**: HIGH
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `PIIDetectionGuard`
- [ ] Implement credit card detection
- [ ] Implement SSN detection
- [ ] Implement password pattern detection
- [ ] Create redaction utilities
- [ ] Configure PII patterns

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/content/PIIDetectionGuard.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/security/PIIDetector.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/security/RedactionUtils.kt`

### 2.2 Semantic Validation
**Priority**: HIGH
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `SemanticGuard` interface
- [ ] Implement `PromptInjectionGuard` (detects "ignore previous instructions")
- [ ] Implement `JailbreakDetectionGuard` (detects DAN mode, etc.)
- [ ] Implement `SocialEngineeringGuard` (detects manipulation attempts)
- [ ] Create pattern library for detection

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/semantic/SemanticGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/semantic/PromptInjectionGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/semantic/JailbreakDetectionGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/semantic/SocialEngineeringGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/semantic/PatternLibrary.kt`

### 2.3 Behavioral Anomaly Detection
**Priority**: MEDIUM
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `AnomalyDetectionGuard`
- [ ] Implement usage pattern tracking
- [ ] Implement spike detection
- [ ] Implement repetitive failure detection
- [ ] Create anomaly scoring system

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/behavioral/AnomalyDetectionGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/behavioral/UsagePatternTracker.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/behavioral/AnomalyScorer.kt`

### 2.4 Conversation Context Guards
**Priority**: MEDIUM
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `ConversationContextGuard`
- [ ] Implement tool appropriateness checker
- [ ] Implement intent alignment validator
- [ ] Implement repetition detector
- [ ] Integrate with conversation history

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/context/ConversationContextGuard.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/context/ToolAppropriatenessChecker.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/context/IntentAlignmentValidator.kt`

## Phase 3: Monitoring & Audit (Week 4)

### 3.1 Guard Violation Logging
**Priority**: HIGH
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `GuardViolationLogger` interface
- [ ] Implement database-backed logger
- [ ] Create guard_violations table migration
- [ ] Implement violation categorization
- [ ] Create violation dashboard queries

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/guard/GuardViolationLogger.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/logging/DatabaseGuardViolationLogger.kt`
- `app/src/main/resources/db/tenant-schema/V14__create_guard_violations_table.sql`

### 3.2 Audit Trail
**Priority**: HIGH
**Dependencies**: 1.1

**Tasks**:
- [ ] Create `ToolExecutionAuditLogger`
- [ ] Create tool_executions table migration
- [ ] Log all tool executions with context
- [ ] Create audit trail API endpoints
- [ ] Add audit trail to admin dashboard

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/audit/ToolExecutionAuditLogger.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/audit/DatabaseAuditLogger.kt`
- `app/src/main/resources/db/tenant-schema/V15__create_tool_executions_table.sql`

### 3.3 Real-Time Alerting
**Priority**: MEDIUM
**Dependencies**: 3.1

**Tasks**:
- [ ] Create `GuardAlertService`
- [ ] Implement threshold-based alerts
- [ ] Implement security incident alerts
- [ ] Integrate with notification system
- [ ] Create alert configuration

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/alert/GuardAlertService.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/alert/AlertConfiguration.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/guard/alert/AlertNotifier.kt`

## Phase 4: Tool Framework (Week 5-6)

### 4.1 Core Tool Abstractions
**Priority**: MEDIUM
**Dependencies**: Phase 3 complete

**Tasks**:
- [ ] Create `Tool` interface
- [ ] Create `ToolContext` data class
- [ ] Create `ToolResult` sealed class
- [ ] Create `ToolParameter` data class
- [ ] Create `ToolMetadata` data class
- [ ] Refactor EmailTool to implement new interface

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/Tool.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/ToolContext.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/ToolResult.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/ToolParameter.kt`
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/ToolMetadata.kt`

### 4.2 Composite Tool Pattern
**Priority**: LOW
**Dependencies**: 4.1

**Tasks**:
- [ ] Create `CompositeTool` interface
- [ ] Create `WorkflowTool` (sequential execution)
- [ ] Create `ConditionalTool` (conditional execution)
- [ ] Create `ParallelTool` (concurrent execution)
- [ ] Create workflow DSL

**Files to Create**:
- `commons/src/main/kotlin/nl/compilot/ai/commons/tool/CompositeTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/workflow/WorkflowTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/workflow/ConditionalTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/workflow/ParallelTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/workflow/WorkflowDSL.kt`

### 4.3 Tool Registry & Discovery
**Priority**: LOW
**Dependencies**: 4.1

**Tasks**:
- [ ] Create `ToolRegistry` service
- [ ] Implement tool registration
- [ ] Implement tool discovery by capability
- [ ] Implement tool versioning
- [ ] Create tool catalog API

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/ToolRegistry.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/ToolDiscoveryService.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tool/ToolCatalogApi.kt`

### 4.4 Additional Tool Implementations
**Priority**: LOW
**Dependencies**: 4.1

**Tasks**:
- [ ] Create `CreateTicketTool`
- [ ] Create `SendSMSTool`
- [ ] Create `WebhookTool`
- [ ] Create `EscalationWorkflowTool` (composite)
- [ ] Apply guards to all new tools

**Files to Create**:
- `core-ai/src/main/kotlin/nl/compilot/ai/tools/CreateTicketTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tools/SendSMSTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tools/WebhookTool.kt`
- `core-ai/src/main/kotlin/nl/compilot/ai/tools/workflow/EscalationWorkflowTool.kt`

## Phase 5: Advanced Features (Week 7+)

### 5.1 ML-Based Anomaly Detection
**Priority**: LOW
**Dependencies**: 2.3

**Tasks**:
- [ ] Train anomaly detection model
- [ ] Implement ML-based guard
- [ ] Create feedback loop for model improvement
- [ ] Deploy model service

### 5.2 Emergency Controls
**Priority**: MEDIUM
**Dependencies**: Phase 3 complete

**Tasks**:
- [ ] Create kill switch mechanism
- [ ] Implement global tool disable
- [ ] Create emergency rate limiting
- [ ] Create session quarantine
- [ ] Create admin control panel

### 5.3 Cost Controls
**Priority**: MEDIUM
**Dependencies**: 3.2

**Tasks**:
- [ ] Implement token usage tracking
- [ ] Create budget limits per tenant
- [ ] Implement cost estimation for tools
- [ ] Create cost alerts
- [ ] Create billing integration

## Guard Configuration Schema

```yaml
guards:
  rate-limiting:
    email:
      per-session: 5
      per-user-daily: 20
      cooldown-seconds: 30
    escalation:
      per-session: 3
      per-user-daily: 10
    search:
      per-session: 100
      per-user-daily: 1000

  authorization:
    anonymous-permissions: [READ]
    authenticated-permissions: [READ, WRITE, COMMUNICATE]
    admin-permissions: [READ, WRITE, COMMUNICATE, ESCALATE, ADMIN]

  validation:
    email:
      max-length: 254
      require-tld: true
    phone:
      formats: [E164, INTERNATIONAL]
      allowed-countries: [NL, BE, DE, FR, GB, US]
    profanity:
      enabled: true
      strict-mode: false

  content:
    pii-detection:
      enabled: true
      redact: true
      patterns:
        - credit-card
        - ssn
        - passport
        - api-key

  behavioral:
    anomaly-threshold: 0.85
    spike-multiplier: 5
    failure-threshold: 3

  monitoring:
    log-all-executions: true
    log-violations: true
    alert-on-critical: true
    alert-threshold: 10
```

## Database Schema

### guard_violations Table
```sql
CREATE TABLE guard_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    user_id VARCHAR(255),
    guard_type VARCHAR(100) NOT NULL,
    guard_name VARCHAR(255) NOT NULL,
    tool_name VARCHAR(255) NOT NULL,
    violation_reason TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    context JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_created (tenant_id, created_at),
    INDEX idx_guard_type (guard_type),
    INDEX idx_severity (severity)
);
```

### tool_executions Table
```sql
CREATE TABLE tool_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    user_id VARCHAR(255),
    tool_name VARCHAR(255) NOT NULL,
    tool_version VARCHAR(50),
    parameters JSONB,
    result_status VARCHAR(50) NOT NULL, -- SUCCESS, FAILURE, BLOCKED
    result_data JSONB,
    execution_time_ms BIGINT,
    token_count INT,
    cost_estimate DECIMAL(10, 6),
    guards_applied JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_created (tenant_id, created_at),
    INDEX idx_tool_name (tool_name),
    INDEX idx_result_status (result_status)
);
```

## Testing Strategy

### Unit Tests
- Test each guard individually
- Test guard chains
- Test guard violation handling
- Test rate limit state management

### Integration Tests
- Test EmailTool with all guards
- Test guard interaction with LangChain4j
- Test tenant isolation
- Test rate limit enforcement

### Load Tests
- Test rate limiting under load
- Test concurrent guard execution
- Test Redis-backed state performance

### Security Tests
- Test prompt injection detection
- Test jailbreak prevention
- Test PII redaction
- Test authorization bypass attempts

## Rollout Strategy

### Week 1-2: Critical Guards (Phase 1)
- Deploy to development environment
- Test with EmailTool
- Monitor guard violations
- Adjust thresholds

### Week 3: Content Guards (Phase 2)
- Deploy to staging environment
- Run security tests
- Validate PII redaction
- Test semantic guards

### Week 4: Monitoring (Phase 3)
- Enable audit logging
- Create admin dashboard
- Set up alerting
- Monitor production metrics

### Week 5-6: Tool Framework (Phase 4)
- Refactor existing tools
- Implement new tools
- Create workflow tools
- Update documentation

### Week 7+: Advanced Features (Phase 5)
- Deploy ML models
- Enable emergency controls
- Implement cost tracking
- Optimize performance

## Success Metrics

- Guard violation rate < 1% of tool executions
- Zero unauthorized data access
- Zero PII leaks
- Zero prompt injection successes
- Rate limit effectiveness > 99%
- Average guard execution time < 10ms
- System uptime > 99.9%

## Risks & Mitigations

### Risk: Performance Impact
**Mitigation**:
- Use Redis for fast state lookups
- Cache guard results
- Optimize guard execution order (fail fast)
- Parallel guard execution where possible

### Risk: False Positives
**Mitigation**:
- Tunable thresholds
- Whitelist mechanism
- Admin override capability
- Continuous monitoring and adjustment

### Risk: Guard Bypass
**Mitigation**:
- Multiple layers of guards
- Audit all tool executions
- Regular security reviews
- Penetration testing

### Risk: Complexity
**Mitigation**:
- Clear documentation
- Simple configuration
- Default secure settings
- Progressive enhancement
