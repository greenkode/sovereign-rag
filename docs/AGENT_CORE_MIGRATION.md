# Agent Core Module Extraction - Migration Guide

## Overview

This document outlines the plan to extract the core AI agent functionality into reusable modules, separating concerns and enabling the agent framework to be used independently of RAG, multi-tenancy, and language-specific features.

## Goals

1. **Reusability**: Create a framework-agnostic core agent that can be used in any project
2. **Modularity**: Separate optional features (RAG, multilingual, multi-tenant) into extensions
3. **Testability**: Enable independent testing of each module
4. **Maintainability**: Clear separation of concerns with well-defined interfaces
5. **No Lock-in**: Core module has no Spring, database, or infrastructure dependencies

## Target Module Structure

```
compilot-ai/
├── core-ms/
│   ├── agent-core/          # NEW: Reusable agent framework
│   │   ├── agent/           # Core agent interfaces and implementations
│   │   ├── memory/          # Conversation memory
│   │   ├── guardrail/       # Security guardrails (pattern-based)
│   │   ├── tool/            # Tool execution framework
│   │   ├── persona/         # Persona system
│   │   ├── model/           # LLM provider abstraction
│   │   └── util/            # Utilities
│   │
│   ├── agent-extensions/    # NEW: Optional feature extensions
│   │   ├── rag/             # RAG (Retrieval-Augmented Generation)
│   │   ├── multilingual/    # Multi-language support
│   │   ├── guardrail/       # Advanced guardrails (LLM-based)
│   │   └── multitenant/     # Multi-tenancy support
│   │
│   ├── core-ai/             # REFACTORED: Application-specific logic
│   │   ├── api/             # REST controllers
│   │   ├── application/     # Agent composition & configuration
│   │   ├── content/         # Content management (ingestion)
│   │   ├── tenant/          # Tenant management
│   │   └── config/          # Spring configuration
│   │
│   ├── commons/             # Existing: Shared utilities
│   └── app/                 # Existing: Main application
```

---

## Package Mapping

### agent-core Module

| Current Location | New Location | Notes |
|-----------------|--------------|-------|
| `chat/ConversationalAgentService.kt` | `agent-core/agent/ConversationalAgent.kt` | Extract core logic, remove RAG/tenant/language |
| `chat/ChatSession.kt` | `agent-core/agent/AgentSession.kt` | Simplify, remove tenant/language fields |
| `chat/SerializableChatMemory.kt` | `agent-core/memory/SerializableChatMemory.kt` | Move to memory package |
| `guardrail/InputGuardrail.kt` | `agent-core/guardrail/InputGuardrail.kt` | Keep interface as-is |
| `guardrail/OutputGuardrail.kt` | `agent-core/guardrail/OutputGuardrail.kt` | Keep interface as-is |
| `guardrail/GuardrailResult.kt` | `agent-core/guardrail/GuardrailResult.kt` | Keep sealed class as-is |
| `guardrail/PromptInjectionGuardrail.kt` | `agent-core/guardrail/impl/PromptInjectionGuardrail.kt` | Pattern-based, no LLM |
| `guardrail/JailbreakDetectionGuardrail.kt` | `agent-core/guardrail/impl/JailbreakDetectionGuardrail.kt` | Pattern-based, no LLM |
| `guardrail/PIIDetectionInputGuardrail.kt` | `agent-core/guardrail/impl/PIIDetectionGuardrail.kt` | Rename, pattern-based |
| `guardrail/PIIRedactionOutputGuardrail.kt` | `agent-core/guardrail/impl/PIIRedactionGuardrail.kt` | Pattern-based |
| `tools/EmailTool.kt` | Extract pattern → `agent-core/tool/Tool.kt` | Create generic interface |
| `config/CompilotProperties.kt` (persona config) | `agent-core/persona/Persona.kt` | Extract persona model |

### agent-extensions Module

| Current Location | New Location | Notes |
|-----------------|--------------|-------|
| `content/service/ContentService.kt` | `agent-extensions/rag/ContentRetriever.kt` | Extract RAG logic |
| `service/RerankerService.kt` | `agent-extensions/rag/RerankerService.kt` | Keep as-is |
| `content/retriever/TenantContentRetriever.kt` | `agent-extensions/rag/ContentRetrieverImpl.kt` | Make tenant-agnostic |
| `ConversationalAgentService.detectLanguage()` | `agent-extensions/multilingual/LanguageDetector.kt` | Extract language detection |
| `guardrail/LlmSemanticGuardrail.kt` | `agent-extensions/guardrail/LlmSemanticGuardrail.kt` | LLM-based, requires chat model |
| `tenant/` (core logic) | `agent-extensions/multitenant/TenantContext.kt` | Extract tenant routing pattern |
| `content/store/TenantAwarePgVectorStoreFactory.kt` | `agent-extensions/multitenant/TenantAwareStoreFactory.kt` | Generic pattern |

### core-ai Module (Refactored)

| Current Location | New Location | Notes |
|-----------------|--------------|-------|
| `chat/ChatController.kt` | `core-ai/api/ChatController.kt` | Use agent-core interfaces |
| `content/api/IngestController.kt` | `core-ai/api/IngestController.kt` | Keep ingestion endpoints |
| NEW | `core-ai/application/CompilotAgent.kt` | Compose core + extensions |
| NEW | `core-ai/application/CompilotAgentFactory.kt` | Factory for configured agents |
| `content/service/ContentService.kt` | `core-ai/content/service/ContentManagementService.kt` | Ingestion orchestration only |
| `tenant/service/TenantService.kt` | `core-ai/tenant/service/TenantService.kt` | Keep tenant CRUD |
| `config/LangChain4jConfig.kt` | `core-ai/config/AgentConfiguration.kt` | Wire up modules |

---

## Migration Phases

### Phase 1: Create agent-core Module (Week 1-2)

**Goal**: Extract core agent framework with no external dependencies

#### Step 1.1: Create Module Structure
- [ ] Create `core-ms/agent-core/` directory
- [ ] Create `pom.xml` with minimal dependencies (Kotlin, Coroutines, LangChain4j-core)
- [ ] Add module to root `pom.xml`
- [ ] Create package structure:
  - [ ] `nl.compilot.agent.core.agent`
  - [ ] `nl.compilot.agent.core.memory`
  - [ ] `nl.compilot.agent.core.guardrail`
  - [ ] `nl.compilot.agent.core.tool`
  - [ ] `nl.compilot.agent.core.persona`
  - [ ] `nl.compilot.agent.core.model`
  - [ ] `nl.compilot.agent.core.util`

#### Step 1.2: Define Core Interfaces
- [ ] Create `Agent.kt` interface
  ```kotlin
  interface Agent {
      suspend fun chat(message: String, context: AgentContext = AgentContext()): AgentResponse
      fun getConfig(): AgentConfig
  }
  ```
- [ ] Create `AgentContext.kt` data class
- [ ] Create `AgentResponse.kt` data class
- [ ] Create `AgentConfig.kt` configuration class

#### Step 1.3: Implement Memory System
- [ ] Create `ChatMemory.kt` interface
- [ ] Create `ChatMessage.kt` data class
- [ ] Create `InMemoryChatMemory.kt` implementation
- [ ] Copy `SerializableChatMemory.kt` from `core-ai/chat/`
- [ ] Remove tenant-specific logic from `SerializableChatMemory`

#### Step 1.4: Extract Guardrail System
- [ ] Copy `InputGuardrail.kt` interface
- [ ] Copy `OutputGuardrail.kt` interface
- [ ] Copy `GuardrailResult.kt` sealed class
- [ ] Create `GuardrailChain.kt` for composing guardrails
- [ ] Create `guardrail/impl/` package
- [ ] Copy and clean up pattern-based guardrails:
  - [ ] `PromptInjectionGuardrail.kt`
  - [ ] `JailbreakDetectionGuardrail.kt`
  - [ ] `PIIDetectionGuardrail.kt` (rename from PIIDetectionInputGuardrail)
  - [ ] `PIIRedactionGuardrail.kt` (rename from PIIRedactionOutputGuardrail)
- [ ] Remove Spring annotations (@Component, @Service)
- [ ] Add constructor dependency injection

#### Step 1.5: Create Tool System
- [ ] Create `Tool.kt` interface
  ```kotlin
  interface Tool {
      fun name(): String
      fun description(): String
      suspend fun execute(parameters: Map<String, Any>): ToolResult
      fun parametersSchema(): String
  }
  ```
- [ ] Create `ToolResult.kt` sealed class
- [ ] Create `ToolRegistry.kt` for tool management
- [ ] Create `ToolExecutor.kt` for tool invocation

#### Step 1.6: Implement Persona System
- [ ] Create `Persona.kt` data class
- [ ] Create `PersonaBehavior.kt` for behavior configuration
- [ ] Create `PersonaBuilder.kt` fluent builder
- [ ] Extract persona definitions from `CompilotProperties.kt`

#### Step 1.7: Create LLM Provider Abstraction
- [ ] Create `LLMProvider.kt` interface
- [ ] Create `LLMRequest.kt` data class
- [ ] Create `LLMResponse.kt` data class
- [ ] Create `LangChain4jProvider.kt` implementation
- [ ] Support streaming responses

#### Step 1.8: Implement Core Agent
- [ ] Create `ConversationalAgent.kt`
- [ ] Extract core logic from `ConversationalAgentService.kt`:
  - [ ] Conversation flow management
  - [ ] Guardrail integration (input → LLM → output)
  - [ ] Tool execution
  - [ ] Persona-based prompt generation
  - [ ] Memory management
- [ ] Remove RAG integration
- [ ] Remove language detection
- [ ] Remove tenant context
- [ ] Remove Spring dependencies

#### Step 1.9: Write Unit Tests
- [ ] Test `ConversationalAgent` with mock LLM
- [ ] Test guardrail chain execution
- [ ] Test tool registry and execution
- [ ] Test persona builder
- [ ] Test memory implementations
- [ ] Achieve 80%+ code coverage

#### Step 1.10: Documentation
- [ ] Write `agent-core/README.md` with:
  - [ ] Quick start guide
  - [ ] Architecture overview
  - [ ] API documentation
  - [ ] Usage examples
- [ ] Write inline KDoc for all public APIs

---

### Phase 2: Create agent-extensions Module (Week 3-4)

**Goal**: Extract optional features into extension modules

#### Step 2.1: Create Module Structure
- [ ] Create `core-ms/agent-extensions/` directory
- [ ] Create `pom.xml` with optional dependencies
- [ ] Add module to root `pom.xml`
- [ ] Add dependency on `agent-core`
- [ ] Create package structure:
  - [ ] `nl.compilot.agent.extensions.rag`
  - [ ] `nl.compilot.agent.extensions.multilingual`
  - [ ] `nl.compilot.agent.extensions.guardrail`
  - [ ] `nl.compilot.agent.extensions.multitenant`

#### Step 2.2: RAG Extension
- [ ] Create `RAGAgent.kt` (delegates to base agent)
- [ ] Create `ContentRetriever.kt` interface
- [ ] Extract search logic from `ContentService.kt`
- [ ] Copy `RerankerService.kt`
- [ ] Create `RAGConfig.kt` for configuration
- [ ] Remove tenant-specific logic
- [ ] Support custom retrievers

#### Step 2.3: Multilingual Extension
- [ ] Create `MultilingualAgent.kt` (delegates to base agent)
- [ ] Create `LanguageDetector.kt` interface
- [ ] Extract `detectLanguage()` from `ConversationalAgentService.kt`
- [ ] Create `LanguageTranslator.kt` interface (optional)
- [ ] Support 13+ languages
- [ ] Create `MultilingualConfig.kt`

#### Step 2.4: Advanced Guardrail Extension
- [ ] Copy `LlmSemanticGuardrail.kt`
- [ ] Make it work with agent-core guardrail interfaces
- [ ] Support custom LLM providers
- [ ] Create configuration class

#### Step 2.5: Multi-tenant Extension
- [ ] Create `TenantAwareAgent.kt` (delegates to base agent)
- [ ] Create `TenantContext.kt` (ThreadLocal pattern)
- [ ] Create `TenantResolver.kt` interface
- [ ] Extract tenant routing pattern from `TenantDataSourceRouter.kt`
- [ ] Create `TenantAwareStoreFactory.kt` pattern
- [ ] Remove Spring/JPA dependencies

#### Step 2.6: Write Integration Tests
- [ ] Test RAG agent with mock retriever
- [ ] Test multilingual agent with multiple languages
- [ ] Test semantic guardrail with mock LLM
- [ ] Test tenant-aware agent with mock tenant resolver
- [ ] Test composition of multiple extensions

#### Step 2.7: Documentation
- [ ] Write `agent-extensions/README.md`
- [ ] Document each extension with examples
- [ ] Document extension composition patterns

---

### Phase 3: Refactor core-ai Module (Week 5-6)

**Goal**: Update core-ai to use new modules

#### Step 3.1: Update Dependencies
- [ ] Add dependency on `agent-core`
- [ ] Add dependency on `agent-extensions`
- [ ] Update `pom.xml`

#### Step 3.2: Create Agent Factory
- [ ] Create `core-ai/application/CompilotAgentFactory.kt`
- [ ] Wire up agent-core + extensions
- [ ] Configure guardrails (pattern + LLM-based)
- [ ] Configure tools (EmailTool, SearchTool)
- [ ] Configure personas
- [ ] Configure RAG extension
- [ ] Configure multilingual extension
- [ ] Configure multi-tenant extension

#### Step 3.3: Create Composed Agent
- [ ] Create `core-ai/application/CompilotAgent.kt`
- [ ] Compose: `ConversationalAgent` → `RAGAgent` → `MultilingualAgent` → `TenantAwareAgent`
- [ ] Delegate pattern for clean composition

#### Step 3.4: Update Controllers
- [ ] Update `ChatController.kt` to use `CompilotAgentFactory`
- [ ] Update `AskQueryHandler.kt` to use agent-core interfaces
- [ ] Update `SendMessageCommandHandler.kt` to use agent-core
- [ ] Remove direct dependencies on `ConversationalAgentService`

#### Step 3.5: Update Configuration
- [ ] Create `AgentConfiguration.kt` Spring configuration class
- [ ] Wire up LLM providers (Ollama via LangChain4j)
- [ ] Configure guardrails as Spring beans
- [ ] Configure tools as Spring beans
- [ ] Configure memory (Redis-backed)
- [ ] Configure personas from `application.yml`

#### Step 3.6: Migrate Content Management
- [ ] Rename `ContentService.kt` → `ContentManagementService.kt`
- [ ] Focus on ingestion orchestration only
- [ ] Use `agent-extensions/rag/ContentRetriever` for search
- [ ] Keep CQRS commands/queries/handlers

#### Step 3.7: Update Tests
- [ ] Update controller tests to use new agent interfaces
- [ ] Update integration tests
- [ ] Verify RAG functionality
- [ ] Verify multilingual functionality
- [ ] Verify multi-tenant isolation
- [ ] Verify guardrails work end-to-end

#### Step 3.8: Remove Deprecated Code
- [ ] Mark old `ConversationalAgentService.kt` as deprecated
- [ ] Plan removal after migration validation
- [ ] Remove unused imports
- [ ] Clean up dead code

---

### Phase 4: Testing & Validation (Week 7)

**Goal**: Ensure feature parity and stability

#### Step 4.1: Functional Testing
- [ ] Test all chat endpoints
- [ ] Test persona switching
- [ ] Test language detection and translation
- [ ] Test RAG search with different confidence thresholds
- [ ] Test guardrail violations
- [ ] Test tool execution (email escalation)
- [ ] Test multi-tenant isolation

#### Step 4.2: Performance Testing
- [ ] Benchmark agent response times
- [ ] Compare with old implementation
- [ ] Ensure no performance regression
- [ ] Profile memory usage
- [ ] Test concurrent request handling

#### Step 4.3: Integration Testing
- [ ] Test with WordPress plugin
- [ ] Test API authentication
- [ ] Test session management
- [ ] Test Redis integration
- [ ] Test PostgreSQL + pgvector

#### Step 4.4: Security Testing
- [ ] Test guardrail detection rates
- [ ] Test prompt injection attempts
- [ ] Test jailbreak attempts
- [ ] Test PII detection/redaction
- [ ] Test tenant isolation

#### Step 4.5: Compatibility Testing
- [ ] Verify backward compatibility with API contracts
- [ ] Test existing client integrations
- [ ] Verify database schema compatibility
- [ ] Test configuration migration

---

### Phase 5: Documentation & Rollout (Week 8)

**Goal**: Complete documentation and deploy

#### Step 5.1: Update Main README
- [ ] Document new module structure
- [ ] Update architecture diagrams
- [ ] Add migration guide for developers
- [ ] Document breaking changes

#### Step 5.2: Write API Documentation
- [ ] Generate KDoc for agent-core
- [ ] Generate KDoc for agent-extensions
- [ ] Create OpenAPI/Swagger docs for REST APIs
- [ ] Document configuration options

#### Step 5.3: Create Usage Examples
- [ ] Simple agent example (no extensions)
- [ ] RAG agent example
- [ ] Multilingual agent example
- [ ] Multi-tenant agent example
- [ ] Full-featured agent example (all extensions)
- [ ] Custom tool example
- [ ] Custom guardrail example

#### Step 5.4: Write Migration Guide
- [ ] Document how to migrate from old code
- [ ] Provide code examples for common patterns
- [ ] Document breaking changes
- [ ] Provide troubleshooting guide

#### Step 5.5: Release Planning
- [ ] Tag release `v2.0.0` (breaking changes)
- [ ] Publish artifacts to Maven repository (if applicable)
- [ ] Update changelog
- [ ] Notify stakeholders

#### Step 5.6: Deployment
- [ ] Deploy to staging environment
- [ ] Run smoke tests
- [ ] Monitor for issues
- [ ] Deploy to production (gradual rollout)
- [ ] Monitor metrics and logs

---

## Dependency Matrix

### agent-core Module

**Minimal Dependencies:**
```xml
<dependencies>
    <!-- Language -->
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-stdlib</artifactId>
    </dependency>

    <!-- Async -->
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-coroutines-core</artifactId>
    </dependency>

    <!-- LLM Abstraction -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-core</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-test-junit5</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Explicitly NO:**
- ❌ Spring Framework
- ❌ Database drivers
- ❌ Redis
- ❌ Multi-tenancy libraries

### agent-extensions Module

**Optional Dependencies:**
```xml
<dependencies>
    <dependency>
        <groupId>nl.compilot</groupId>
        <artifactId>agent-core</artifactId>
    </dependency>

    <!-- RAG Extension -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-pgvector</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Reranking -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Document Parsing -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-tika</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### core-ai Module

**Application Dependencies:**
```xml
<dependencies>
    <!-- Core Modules -->
    <dependency>
        <groupId>nl.compilot</groupId>
        <artifactId>agent-core</artifactId>
    </dependency>
    <dependency>
        <groupId>nl.compilot</groupId>
        <artifactId>agent-extensions</artifactId>
    </dependency>
    <dependency>
        <groupId>nl.compilot</groupId>
        <artifactId>commons</artifactId>
    </dependency>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- ... other existing dependencies ... -->
</dependencies>
```

---

## Key Design Principles

### 1. Interface-Driven Design
- Define clear interfaces for all abstractions
- Use dependency injection (constructor injection preferred)
- Avoid concrete class dependencies

### 2. Decorator Pattern
- Use decorator/wrapper pattern for extensions
- Each extension wraps the base agent
- Maintains single responsibility principle

### 3. Configuration over Code
- Externalize configuration
- Support multiple configuration sources
- Provide sensible defaults

### 4. Fail-Safe Defaults
- Guardrails default to enabled
- Tools require explicit registration
- Personas have safe defaults

### 5. Observability
- Log all critical operations
- Provide metrics hooks
- Support distributed tracing

---

## Success Criteria

### Functionality
- [ ] All existing features work with new modules
- [ ] No regression in response quality
- [ ] No regression in response time
- [ ] All tests pass (unit, integration, E2E)

### Code Quality
- [ ] 80%+ code coverage for agent-core
- [ ] 70%+ code coverage for agent-extensions
- [ ] No critical SonarQube issues
- [ ] Clear separation of concerns

### Documentation
- [ ] All public APIs documented
- [ ] Usage examples provided
- [ ] Migration guide complete
- [ ] Architecture diagrams updated

### Performance
- [ ] Agent response time < 3s (p95)
- [ ] Memory usage within 10% of baseline
- [ ] Concurrent request handling unchanged

---

## Risks & Mitigations

### Risk 1: Breaking Changes
**Impact**: High
**Probability**: Medium
**Mitigation**:
- Maintain old implementation during migration
- Gradual rollout with feature flags
- Comprehensive testing before removal

### Risk 2: Performance Regression
**Impact**: High
**Probability**: Low
**Mitigation**:
- Benchmark before/after
- Profile critical paths
- Optimize hot paths identified

### Risk 3: Incomplete Feature Extraction
**Impact**: Medium
**Probability**: Medium
**Mitigation**:
- Detailed code analysis before extraction
- Comprehensive test coverage
- Phased migration approach

### Risk 4: Dependency Conflicts
**Impact**: Low
**Probability**: Low
**Mitigation**:
- Use Maven dependency management
- Keep dependencies minimal in agent-core
- Use optional dependencies in extensions

---

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Phase 1: agent-core | 2 weeks | Reusable agent framework |
| Phase 2: agent-extensions | 2 weeks | Optional feature extensions |
| Phase 3: Refactor core-ai | 2 weeks | Integrated application |
| Phase 4: Testing & Validation | 1 week | Validated migration |
| Phase 5: Documentation & Rollout | 1 week | Production deployment |
| **Total** | **8 weeks** | **Modular AI agent system** |

---

## Next Steps

1. **Review & Approve**: Stakeholder review of migration plan
2. **Resource Allocation**: Assign developers to phases
3. **Environment Setup**: Prepare development/testing environments
4. **Kickoff**: Begin Phase 1 implementation
5. **Weekly Check-ins**: Monitor progress and adjust timeline

---

## Questions & Decisions

### Open Questions
- [ ] Should agent-core be published as a separate library?
- [ ] What's the versioning strategy for modules?
- [ ] Should we support multiple LLM providers in agent-core?
- [ ] Do we need a migration CLI tool?

### Key Decisions Required
- [ ] Approval to proceed with extraction
- [ ] Timeline/resource allocation
- [ ] Backward compatibility requirements
- [ ] Release strategy (big bang vs gradual)

---

## Contact

For questions or concerns about this migration:
- **Technical Lead**: [Name]
- **Architecture Review**: [Team/Email]
- **Status Updates**: [Communication Channel]

---

**Last Updated**: 2025-01-11
**Status**: Planning
**Next Review**: [Date]
