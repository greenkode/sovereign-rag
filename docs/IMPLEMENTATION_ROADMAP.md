# Implementation Roadmap

## Overview
This document provides a phased development plan to build Sovereign RAG from its current state to feature parity with Progress Agentic RAG.

---

## Current State Assessment

### ✅ Already Implemented
Based on the existing codebase:

1. **Multi-tenancy** - Database-per-tenant architecture
2. **Basic Document Ingestion** - File upload capability
3. **Vector Storage** - PgVector integration with LangChain4j
4. **LLM Integration** - Ollama support
5. **Embedding Generation** - Local embedding model support
6. **Basic Search** - Hybrid search (vector + full-text)
7. **Tenant Management** - Tenant registration and isolation
8. **Authentication** - JWT and API key auth

### ❌ Missing Features (From Screenshots)
1. Multiple upload types (Links, Sitemap, Q&A, Text)
2. Synchronization system (Folder, RSS, Sitemap)
3. Search configuration UI
4. Widget system
5. Multiple AI model providers (OpenAI, Anthropic, Google)
6. RAG evaluation metrics (REMI)
7. Activity logging
8. Label system
9. User management UI
10. Feedback collection
11. Advanced search options (query rephrasing, reranking)

---

## PHASE 1: CORE ENHANCEMENTS (Weeks 1-4)

### Goal
Enhance existing RAG capabilities and add essential upload types.

### Features

#### 1.1 Enhanced Document Ingestion
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] URL/Link upload endpoint
  - Implement HTML fetching and parsing
  - Extract text content from web pages
  - Handle metadata extraction (title, description, images)

- [ ] Manual text entry endpoint
  - Simple text block creation
  - Markdown support

- [ ] Sitemap crawler
  - XML sitemap parsing
  - Configurable max pages
  - URL filtering

- [ ] Q&A pairs upload
  - Structured question-answer format
  - Optimized chunking for Q&A

**Acceptance Criteria**:
- All 6 upload types functional (File, Folder, Links, Text, Sitemap, Q&A)
- Resources appear in Resources List
- Each type correctly processed and embedded

#### 1.2 Multiple AI Model Providers
**Priority**: P0 (Critical)
**Effort**: 2 weeks

**Tasks**:
- [ ] OpenAI integration (direct, not Azure)
  - API client implementation
  - Model selection (GPT-4o, GPT-4o-mini, etc.)
  - Token counting and cost tracking

- [ ] Anthropic Claude integration
  - API client
  - Support for Claude 3.5, Claude 4.5 models

- [ ] Google Gemini integration
  - API client
  - Support for Gemini Pro, Flash models

- [ ] Model configuration interface
  - Store model selections per Knowledge Box
  - Model parameters (temperature, max_tokens, etc.)

- [ ] Model switching in Search Configuration
  - Override default model per search config

**Acceptance Criteria**:
- At least 3 providers working (OpenAI, Anthropic, Ollama)
- Model selection persists in database
- Different models can be used for different tasks
- Graceful fallback when model unavailable

#### 1.3 Advanced Search Options
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Query rephrasing
  - Use LLM to rephrase user query for better retrieval
  - Configurable enable/disable
  - Custom rephrase prompt option

- [ ] Semantic re-ranking
  - Post-retrieval reranking with cross-encoder
  - Configurable threshold

- [ ] Reciprocal Rank Fusion
  - Combine keyword and vector search scores
  - Implement RRF algorithm

- [ ] Search result filtering
  - Pre-selected filters
  - Dynamic filter UI

**Acceptance Criteria**:
- Query rephrasing improves retrieval quality
- Reranking changes result order measurably
- RRF provides better combined results than either alone

---

## PHASE 2: CONFIGURATION & WIDGETS (Weeks 5-8)

### Goal
Implement search configuration system and embeddable widgets.

### Features

#### 2.1 Search Configuration System
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tasks**:
- [ ] SearchConfiguration entity and repository
- [ ] CRUD API endpoints for configurations
- [ ] Configuration cloning/templates
- [ ] Default configuration per Knowledge Box
- [ ] Configuration versioning

**Configuration Options to Implement**:
- [ ] Search options (use results, filters, rephrasing, reranking, RRF)
- [ ] Generative answer options (enable, model, prompts, token limits)
- [ ] Result display options (citations, metadata, thumbnails, top_k)
- [ ] User-intent routing (dynamic config selection)

**Acceptance Criteria**:
- Multiple configurations can be saved per Knowledge Box
- Configuration can be applied to searches
- UI reflects all configuration options

#### 2.2 Widget System
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tasks**:
- [ ] Widget entity and CRUD endpoints
- [ ] Widget configuration (styles, themes, placeholders)
- [ ] JavaScript widget SDK
  - Embed code generation
  - CDN hosting setup
  - Widget initialization script

- [ ] Widget rendering modes
  - Embedded in page
  - Chat mode
  - Popup modal

- [ ] Widget theming
  - Light theme
  - Dark theme
  - Custom CSS injection

- [ ] Chat history
  - Session management
  - LocalStorage persistence
  - Server-side session storage

**Acceptance Criteria**:
- Widget can be created from saved search config
- Embed code works on external websites
- All 3 widget styles functional
- Chat history persists across page reloads

#### 2.3 Feedback Collection
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Feedback entity and endpoints
- [ ] Widget feedback UI
  - Thumbs up/down buttons
  - Comment field
  - Detailed feedback on results

- [ ] Feedback analytics dashboard
- [ ] Link feedback to REMI metrics

**Acceptance Criteria**:
- Users can provide feedback on answers
- Feedback stored and retrievable
- Negative feedback highlights knowledge gaps

---

## PHASE 3: SYNCHRONIZATION (Weeks 9-11)

### Goal
Enable automatic content synchronization from external sources.

### Features

#### 3.1 Sync Agent Architecture
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] Sync server design (desktop agent + remote server)
- [ ] Sync job scheduler (cron-based)
- [ ] SyncConfiguration entity and API
- [ ] Sync status tracking and logging

#### 3.2 Folder Synchronization
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] File system monitoring
- [ ] Incremental sync (detect changes)
- [ ] File pattern matching (include/exclude)
- [ ] Bi-directional sync handling

**Acceptance Criteria**:
- Desktop agent can watch local folders
- Changes automatically uploaded
- Deleted files removed from Knowledge Box

#### 3.3 RSS & Sitemap Sync
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] RSS feed parser
- [ ] Sitemap crawler (recurring)
- [ ] Change detection (avoid re-indexing)
- [ ] Scheduled execution

**Acceptance Criteria**:
- RSS feeds auto-sync on schedule
- Sitemaps crawled without duplicates
- New content appears automatically

---

## PHASE 4: METRICS & ANALYTICS (Weeks 12-14)

### Goal
Implement RAG evaluation metrics and activity logging.

### Features

#### 4.1 Query Tracking
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Query entity comprehensive logging
- [ ] Performance metrics (response time, token usage)
- [ ] Session grouping for chat history
- [ ] IP and user agent tracking

#### 4.2 RAG Evaluation Metrics (REMI)
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] RAGMetrics entity and computation
- [ ] Answer relevance scoring
  - Use LLM as judge
  - Compare answer to query

- [ ] Context relevance scoring
  - Evaluate retrieved chunks against query

- [ ] Groundedness scoring
  - Check if answer is supported by context

- [ ] Metrics aggregation and visualization
- [ ] Time-series tracking
- [ ] Missing knowledge detection
  - Queries without answers
  - Low context relevance queries
  - Negative feedback queries

**Acceptance Criteria**:
- Metrics computed for each query
- Dashboard shows health status (avg, min, max)
- Performance evolution chart
- Missing knowledge reports actionable

#### 4.3 Activity Log
**Priority**: P3 (Premium feature)
**Effort**: 1 week

**Tasks**:
- [ ] Comprehensive activity logging
- [ ] Export functionality (CSV, JSON)
- [ ] Filtering and pagination
- [ ] Link to REMI metrics

**Acceptance Criteria**:
- All queries logged with full context
- Exportable for offline analysis
- Searchable and filterable

---

## PHASE 5: ADVANCED FEATURES (Weeks 15-18)

### Goal
Implement advanced classification, labeling, and organization features.

### Features

#### 5.1 Label System
**Priority**: P2 (Medium)
**Effort**: 2 weeks

**Tasks**:
- [ ] LabelSet and Label entities
- [ ] Label CRUD endpoints
- [ ] Apply labels to Resources and TextBlocks
- [ ] Exclusive vs. multi-label support
- [ ] Label-based filtering in search
- [ ] Label auto-assignment (ML-based)

**Acceptance Criteria**:
- Labels can be created and managed
- Resources tagged with labels
- Search filters by labels
- Label sets enforce exclusivity rules

#### 5.2 Settings & Configuration UI
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] Knowledge Box settings page
  - Name, description, slug
  - CORS configuration
  - IP whitelisting
  - Hidden resources toggle
  - Publish/unpublish

- [ ] AI models configuration page
  - Model selection per task type
  - Model parameters

- [ ] General settings
  - Zone/region selection
  - Storage limits

**Acceptance Criteria**:
- All settings editable via UI
- Settings persisted correctly
- CORS and IP restrictions enforced

#### 5.3 User Management
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] User invitation system
- [ ] Role-based access control (Reader, Writer, Manager)
- [ ] User list UI
- [ ] Role assignment and updates
- [ ] User removal

**Acceptance Criteria**:
- Users can be invited via email
- Roles enforced on API endpoints
- Users manageable via UI

#### 5.4 API Key Management
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] API key generation with roles
- [ ] Key listing (with prefix only)
- [ ] Key revocation
- [ ] Usage tracking (last used timestamp)

**Acceptance Criteria**:
- API keys created with secure generation
- Keys work for authentication
- Keys can be revoked
- Usage visible in UI

---

## PHASE 6: UI/UX IMPLEMENTATION (Weeks 19-24)

### Goal
Build complete admin dashboard matching Progress Agentic RAG UI.

### Features

#### 6.1 Core Navigation & Layout
**Priority**: P1 (High)
**Effort**: 2 weeks

**Tech Stack Recommendation**:
- **Frontend**: React + TypeScript + Tailwind CSS
- **State Management**: React Query (TanStack Query)
- **Routing**: React Router
- **Forms**: React Hook Form + Zod validation
- **UI Components**: Radix UI or shadcn/ui

**Tasks**:
- [ ] Left sidebar navigation
- [ ] Top navigation bar
- [ ] Responsive layout
- [ ] Theme system (light/dark from screenshots)
- [ ] User avatar and profile menu

#### 6.2 Page Implementations
**Priority**: P1 (High)
**Effort**: 4 weeks

**Pages to Build**:
1. [ ] Home/Dashboard
2. [ ] Upload Data (with 6 upload types)
3. [ ] Resources List (with pagination, filtering)
4. [ ] Synchronize (sync configuration management)
5. [ ] Search (test interface with config panel)
6. [ ] Widgets (list, create, configure)
7. [ ] Settings (Knowledge Box config)
8. [ ] AI Models (model selection across tabs)
9. [ ] Metrics (REMI dashboard with charts)
10. [ ] Activity Log (query history table)
11. [ ] API Keys (key management)
12. [ ] Label Sets (label taxonomy management)
13. [ ] Users (user management table)

**Common Components**:
- [ ] Table with sorting, filtering, pagination
- [ ] Form inputs (text, select, toggle, radio)
- [ ] Modal dialogs
- [ ] Toast notifications
- [ ] Loading states
- [ ] Empty states
- [ ] Collapsible sections
- [ ] Tabs
- [ ] Code snippet display

**Acceptance Criteria**:
- All pages functional and styled
- Matches design from screenshots
- Responsive on mobile/tablet
- Accessible (ARIA labels, keyboard navigation)

---

## PHASE 7: PREMIUM FEATURES (Weeks 25-28)

### Goal
Implement advanced premium features for monetization.

### Features (Tier Gating)

#### 7.1 Billing & Subscription Management
**Priority**: P0 (Critical for SaaS)
**Effort**: 3 weeks

**Description**: Complete billing system with multiple payment providers.

**Tasks**:
- [ ] Database schema for billing
  - Subscription plans table
  - Tenant subscriptions
  - Token usage tracking
  - Invoices and payment methods
  - Billing events log

- [ ] Stripe integration
  - Customer management
  - Subscription lifecycle (create, update, cancel)
  - Metered billing for token overage
  - Webhook handler (subscription events, payment events)
  - Checkout session creation
  - Payment method management

- [ ] Adyen integration (alternative)
  - Payment processing
  - Subscription management
  - Webhook handling

- [ ] PayPal integration (alternative)
  - PayPal Checkout
  - Subscription billing
  - IPN webhook handling

- [ ] Subscription plans
  - Free tier (100K tokens/month, 1 KB)
  - Starter ($29/mo, 1M tokens, 3 KBs)
  - Professional ($99/mo, 5M tokens, 10 KBs)
  - Enterprise ($499/mo, 50M tokens, unlimited KBs)

- [ ] Usage-based billing
  - Real-time token tracking
  - Overage calculation
  - Scheduled usage reporting to payment provider
  - Cost estimation per operation

- [ ] Billing API endpoints
  - Create checkout session
  - Get current subscription
  - View usage and costs
  - Upgrade/downgrade plan
  - Cancel subscription
  - Invoice preview

- [ ] Quota enforcement
  - Check limits before AI operations
  - Soft limits (warnings)
  - Hard limits (blocking)
  - Grace periods for payment failures

- [ ] Invoice generation
  - Auto-generate monthly invoices
  - PDF invoice generation
  - Email delivery
  - Invoice history

**Acceptance Criteria**:
- Users can subscribe via Stripe Checkout
- Token usage tracked for all AI operations
- Overage charges calculated correctly
- Webhooks update subscription status
- Quota limits enforced
- Invoices generated and emailed

#### 7.2 OAuth & SSO (Business Domains Only)
**Priority**: P1 (High for enterprise)
**Effort**: 2 weeks

**Description**: OAuth authentication with Google/Microsoft restricted to business domains.

**Tasks**:
- [ ] OAuth 2.0 server setup
  - Spring Authorization Server or Keycloak
  - Token endpoint
  - Authorization endpoint
  - Refresh token flow

- [ ] Google Workspace integration
  - OAuth client configuration
  - Domain verification
  - Restrict to business domains (no @gmail.com)

- [ ] Microsoft Entra ID (Azure AD) integration
  - OAuth client setup
  - Tenant verification
  - Restrict to organizational accounts

- [ ] Business domain validation
  - Email domain whitelist per tenant
  - Block generic email providers (gmail, outlook, yahoo)
  - Admin-configurable allowed domains

- [ ] User provisioning
  - Auto-create users from OAuth
  - Map OAuth claims to user roles
  - Sync profile information

- [ ] Session management
  - JWT token issuance
  - Refresh token rotation
  - Single sign-out

**Acceptance Criteria**:
- Users can login with Google Workspace
- Users can login with Microsoft 365
- Generic email domains blocked
- Admin can configure allowed domains
- OAuth tokens properly validated

#### 7.3 Agents System
**Priority**: P3 (Nice to have)
**Effort**: 3 weeks

**Description**: Multi-agent system for complex tasks.

**Tasks**:
- [ ] Agent orchestration framework
- [ ] Tool calling interface
- [ ] Agent memory and state
- [ ] Agent templates

#### 7.4 NER (Named Entity Recognition)
**Priority**: P3 (Nice to have)
**Effort**: 1 week

**Tasks**:
- [ ] Entity extraction from documents
- [ ] Entity type classification
- [ ] Entity-based search enhancement

#### 7.5 On-Demand Summarization
**Priority**: P2 (Medium)
**Effort**: 1 week

**Tasks**:
- [ ] Document summarization API
- [ ] Configurable summary length
- [ ] Multi-document summarization

#### 7.6 Task Automation
**Priority**: P3 (Nice to have)
**Effort**: 2 weeks

**Tasks**:
- [ ] Automated labeling
- [ ] Automated Q&A generation from documents
- [ ] Global question answering across KB

---

## PHASE 8: PRODUCTION READINESS (Weeks 29-32)

### Goal
Prepare system for production deployment and scale.

### Features

#### 8.1 Performance Optimization
**Priority**: P0 (Critical)
**Effort**: 2 weeks

**Tasks**:
- [ ] Query optimization (database indices)
- [ ] Connection pool tuning
- [ ] Caching layer (Redis for search results, embeddings)
- [ ] Async processing for ingestion
- [ ] Batch embedding generation
- [ ] CDN for widget JavaScript
- [ ] Response compression

**Targets**:
- Search response < 2 seconds (current: ~1.8s)
- Ingestion < 50ms per chunk (current: ~25ms)
- Support 100 concurrent searches

#### 8.2 Monitoring & Observability
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] Application metrics (Prometheus)
- [ ] Distributed tracing (Jaeger/Zipkin)
- [ ] Logging aggregation (ELK stack or Loki)
- [ ] Health check endpoints
- [ ] Alert configuration

**Metrics to Track**:
- Request latency (p50, p95, p99)
- Error rates
- Token consumption
- Database query performance
- Queue depths

#### 8.3 Database Encryption (TDE)
**Priority**: P1 (High for compliance)
**Effort**: 1 week

**Description**: Transparent Data Encryption for data at rest.

**Tasks**:
- [ ] Percona Server for PostgreSQL setup
  - Enable data-at-rest encryption
  - Key management integration
  - Performance testing

- [ ] Alternative: Native PostgreSQL encryption
  - pgcrypto extension
  - Column-level encryption for sensitive fields
  - Encryption key rotation

- [ ] Alternative: Cloud provider encryption
  - AWS RDS encryption
  - Azure Database encryption
  - Google Cloud SQL encryption

- [ ] Backup encryption
  - Encrypted backups
  - Secure key storage
  - Restore testing

**Acceptance Criteria**:
- Database files encrypted at rest
- No performance degradation >10%
- GDPR/HIPAA compliant
- Key rotation procedures documented

#### 8.4 Security Hardening
**Priority**: P0 (Critical)
**Effort**: 1 week

**Tasks**:
- [ ] Rate limiting (by IP, by API key)
- [ ] Input validation and sanitization
- [ ] SQL injection prevention (use parameterized queries)
- [ ] XSS prevention in widget
- [ ] CORS enforcement
- [ ] API key rotation policy
- [ ] Secret management (HashiCorp Vault or AWS Secrets Manager)
- [ ] Audit logging

#### 8.5 Documentation
**Priority**: P1 (High)
**Effort**: 1 week

**Tasks**:
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Widget integration guide
- [ ] Admin user guide
- [ ] Developer documentation
- [ ] Deployment guide
- [ ] Troubleshooting guide
- [ ] Billing setup guide (Stripe/Adyen/PayPal)
- [ ] OAuth configuration guide

---

## DEPLOYMENT TIMELINE

### Month 1-2: Core Enhancements
- **Week 1-4**: Phase 1 (Document ingestion, AI models, advanced search)
- **Deliverable**: Multi-provider RAG system with 6 upload types

### Month 2-3: Configuration & Widgets
- **Week 5-8**: Phase 2 (Search configs, widgets, feedback)
- **Deliverable**: Embeddable widgets with customization

### Month 3: Synchronization
- **Week 9-11**: Phase 3 (Sync system)
- **Deliverable**: Auto-sync from folders, RSS, sitemaps

### Month 4: Analytics
- **Week 12-14**: Phase 4 (REMI, activity log)
- **Deliverable**: RAG evaluation and monitoring

### Month 4-5: Advanced Features
- **Week 15-18**: Phase 5 (Labels, settings, user mgmt)
- **Deliverable**: Enterprise-ready admin features

### Month 5-6: UI Implementation
- **Week 19-24**: Phase 6 (Full UI)
- **Deliverable**: Complete admin dashboard

### Month 7: Premium Features & Monetization
- **Week 25-28**: Phase 7 (Billing, OAuth/SSO, Agents, NER)
- **Deliverable**: Complete SaaS billing with Stripe/Adyen/PayPal, OAuth for business domains, premium tier features

### Month 8: Production Launch
- **Week 29-32**: Phase 8 (Performance, monitoring, database encryption, security, docs)
- **Deliverable**: Production-ready system with TDE, full observability, hardened security

---

## KEY ARCHITECTURAL DECISIONS

### Knowledge Base Isolation Strategy

**Decision**: Use **hybrid schema-per-tenant + table-per-KB** approach.

**Rationale**:
- Each tenant gets their own database schema (current implementation)
- Within each tenant schema, each Knowledge Base gets separate tables
- Balances isolation, performance, and management complexity

**Implementation**:

```sql
-- Tenant database: sovereignrag_tenant_<tenant_id>

-- Knowledge Base catalog
CREATE TABLE knowledge_bases (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500),
    created_at TIMESTAMP
);

-- Per-KB tables (dynamically created)
CREATE TABLE kb_<kb_id>_embeddings (
    id UUID PRIMARY KEY,
    content TEXT,
    embedding vector(1536),
    metadata JSONB
);

CREATE TABLE kb_<kb_id>_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(500),
    source_url TEXT,
    status VARCHAR(50)
);
```

**Alternatives considered**:

1. **Schema-per-KB** (`sovereignrag_kb_<tenant_id>_<kb_id>`)
   - ✅ Maximum isolation
   - ❌ Connection pool complexity
   - ❌ Migration management overhead
   - ❌ Database proliferation

2. **Single table with KB discriminator**
   - ✅ Simple management
   - ✅ Easy migrations
   - ❌ Complex queries with large datasets
   - ❌ Harder to scale individual KBs

**Chosen approach benefits**:
- Tenant-level isolation (existing)
- Multiple KBs per tenant without new schemas
- Easy to backup/restore individual KBs (pg_dump table)
- Better query performance than single-table
- Simpler than schema-per-KB

### OAuth Business Domain Restriction

**Requirement**: Only allow OAuth login from business email domains.

**Implementation approach**:
- Validate email domain during OAuth callback
- Maintain blocked domain list: `gmail.com`, `outlook.com`, `yahoo.com`, `hotmail.com`, etc.
- Allow tenant admins to configure allowed domains
- Reject authentication for generic email providers

**Example validation**:
```kotlin
fun isBusinessDomain(email: String): Boolean {
    val domain = email.substringAfter("@")
    val blockedDomains = setOf("gmail.com", "outlook.com", "yahoo.com", "hotmail.com")
    return domain !in blockedDomains
}
```

### Database Encryption (TDE)

**Options**:
1. **Percona Server for PostgreSQL** - Preferred for open-source
2. **Native PostgreSQL pgcrypto** - Column-level encryption
3. **Cloud provider encryption** - AWS RDS, Azure Database, GCP Cloud SQL

**Recommendation**: Start with cloud provider encryption (easiest), migrate to Percona if compliance requires it.

### Token Usage Tracking

**Requirement**: Track all AI operations for billing.

**Implementation**:
- Intercept all LLM/embedding calls
- Record token counts to `token_usage` table
- Aggregate hourly for Stripe metering
- Calculate costs based on model pricing

**Models supported**:
- GPT-4 Turbo: $10/$30 per 1M tokens
- Claude 3 Opus: $15/$75 per 1M tokens
- Claude 3 Sonnet: $3/$15 per 1M tokens
- Text embeddings: $0.02-$0.13 per 1M tokens

---

## RESOURCE REQUIREMENTS

### Team Composition (Recommended)

1. **Backend Engineer** (2x)
   - Spring Boot/Kotlin expertise
   - LLM integration experience
   - Database optimization

2. **Frontend Engineer** (1x)
   - React/TypeScript
   - UI/UX implementation

3. **Full-Stack Engineer** (1x)
   - API + UI work
   - Widget development

4. **DevOps Engineer** (0.5x)
   - Infrastructure setup
   - CI/CD pipelines
   - Monitoring

5. **Product Manager** (0.5x)
   - Feature prioritization
   - User acceptance testing

### Infrastructure

**Development**:
- PostgreSQL (with pgvector)
- Redis (caching)
- Ollama (local LLM testing)
- Docker Compose

**Production** (estimate for 1000 concurrent users):
- PostgreSQL: 4 vCPU, 16GB RAM
- Application servers: 3x (4 vCPU, 8GB RAM each)
- Redis: 2 vCPU, 4GB RAM
- Load balancer
- CDN for widget JavaScript
- Object storage (S3) for documents

**External Services**:
- OpenAI API credits
- Anthropic API credits
- Google Cloud AI credits
- Email service (SendGrid/AWS SES)
- Payment processing (Stripe/Adyen/PayPal)
- OAuth providers (Google Workspace, Microsoft Entra ID)

---

## RISK MITIGATION

### High-Risk Areas

1. **Vector Search Performance**
   - **Risk**: Slow searches with large datasets
   - **Mitigation**: IVFFlat indexing, result caching, query optimization

2. **LLM API Costs**
   - **Risk**: Unpredictable token costs
   - **Mitigation**: Token limits, model selection strategy, caching answers

3. **Widget Security**
   - **Risk**: XSS attacks, data leakage
   - **Mitigation**: Content Security Policy, input sanitization, CORS restrictions

4. **Multi-Tenancy Isolation**
   - **Risk**: Cross-tenant data access
   - **Mitigation**: Row-level security, thorough testing, security audits

5. **Scaling Database per Tenant**
   - **Risk**: Connection pool exhaustion
   - **Mitigation**: Connection pooling tuning, database sharding plan

---

## SUCCESS METRICS

### Phase 1-2 (Months 1-2)
- ✅ All 6 upload types working
- ✅ 3+ AI model providers integrated
- ✅ Widgets embeddable on external sites

### Phase 3-4 (Months 3-4)
- ✅ Sync system processing 1000+ documents/hour
- ✅ REMI metrics computed for 95%+ of queries
- ✅ Average answer relevance > 0.80

### Phase 5-6 (Months 5-6)
- ✅ Full UI implemented matching screenshots
- ✅ 10+ active Knowledge Boxes in testing
- ✅ User feedback system capturing 20%+ response rate

### Phase 7-8 (Months 7-8)
- ✅ Production deployment successful
- ✅ Search latency < 2s at p95
- ✅ 99.9% uptime
- ✅ Documentation complete

---

## CONCLUSION

This 8-month roadmap provides a structured path to replicate the Progress Agentic RAG product. The phased approach allows for:

1. **Incremental Value**: Each phase delivers working features
2. **Risk Management**: Critical features prioritized early
3. **Flexibility**: Can adjust based on user feedback
4. **Quality**: Time for testing and optimization

**Next Steps**:
1. Review and approve roadmap
2. Assemble development team
3. Set up development environment
4. Begin Phase 1 implementation
