# LegisTrack – AI Development Guide

## System Overview
**Purpose**: Automate US legislative tracking & AI analysis  
**Flow**: Congress.gov → Ingestion → Postgres → AI Analysis (Ollama) → REST API → React UI → User

## Core Principles
**Decision Priority Ladder** (ALWAYS follow this order):
1. **[SEC]** Security - No secrets in code/logs, input validation
2. **[DATA]** Data Integrity - Consistent persistence, migrations 
3. **[CORR]** Correctness - Type safety, error handling
4. **[REL]** Reliability - Circuit breakers, retries, health checks
5. **[PERF]** Performance - Caching, pagination, indexing
6. **[MAIN]** Maintainability - Clean architecture, testing
7. **[VELO]** Development Velocity - DX improvements
8. **[STYLE]** Code Style - Formatting, conventions

**Rule**: Never sacrifice a higher tier for a lower one. Cite tags (e.g. [DATA]) in PR rationale when trade-offs emerge.

## Architecture Overview

### Module Structure
**Multi-module Gradle project** with clean separation of concerns:

| Module | Responsibility | Key Rules |
|--------|---------------|-----------|
| `core-domain/` | Pure business objects + service ports | No Spring/persistence/HTTP types |
| `persistence-jpa/` | JPA entities, repositories, migrations | Flyway migrations in `src/main/resources/db/migration` |
| `ingestion/` | Scheduled + manual data ingestion | Uses WebClient with suspend + retry + caching |
| `external-congress-adapter/` | Congress.gov API integration | HTTP concerns isolated here |
| `external-ollama-adapter/` | Ollama AI service integration | HTTP concerns isolated here |
| `ai-analysis/` | Prompt builders + AI orchestration | Deterministic, model from config (`app.ollama.model`) |
| `api-rest/` | REST controllers, DTOs, error handling | Runtime fat jar, health endpoints |
| `frontend/` | React + TypeScript UI | React Query hooks only (no raw fetch) |
| `obsidian/LegisTrack/` | Internal documentation | ADRs, runbooks - update only inside vault |

**Runtime**: `api-rest` produces the deployable fat jar

## Core Workflows

### Data Ingestion Pipeline
**Trigger Options**:
- Scheduled: `ScheduledDataIngestionService` (cron)
- Manual: `POST /api/documents/ingest`

**Process Flow**:
1. **Date Calculation**: Compute `fromDate` using config lookback days + injected `Clock` (for test determinism)
2. **API Fetching**: `CongressApiService` with:
   - Suspend WebClient 
   - Exponential backoff (≥2s base + jitter)
   - Adaptive retry suppression when remaining rate % < threshold (default 10%)
3. **Parsing**: Typed DTOs (never raw Map parsing, enforce required params like chamber)
4. **Domain Mapping**: Build `Document` aggregate (sponsors, actions, nullable `actionDate`)
5. **Persistence**: Idempotent upsert via JPA (ensure unique constraints in migrations)
6. **AI Analysis**: Triggered separately (keeps ingestion fast and quota-friendly)

**Resilience Features**:
- **Circuit Breaker**: Tracks consecutive failures, opens after threshold, cooldown for half-open trials
- **Rate Monitoring**: Breaker & rate snapshots exposed via `/api/system/health`
- **Caching**: `@Cacheable` on Congress calls with complete parameter keys (`method_param1_param2_...`)

### AI Analysis Workflow
**Trigger**: `POST /api/documents/{id}/analyze`

**Process**:
1. **Prechecks**: Verify document exists + model availability (`OllamaService.isModelAvailable`)
2. **Prompt Building**: Use existing prompt builder functions (extend, don't concatenate ad-hoc)
3. **Analysis**: Keep deterministic (randomness flags must be config-driven)
4. **Persistence**: Store `AiAnalysis` linked to `Document` with timestamps
5. **Versioning**: Follow existing pattern (version or overwrite - no silent history drops without ADR)

## Data Layer Standards

### Database Migrations
**Critical Rules**:
- **NEVER edit existing Flyway files** - Always create new `V{next}__description.sql` (snake_case)
- **Index Strategy**: Add indexes for all query patterns (search filters, ordering fields)
- **Audit Fields**: All entities need `createdAt`, `updatedAt` (follow current implementation)

**Architecture Rules**:
- Keep domain model free of JPA annotations (mapping layer handles translation)
- Never leak persistence types (EntityManager, JPA entities) outside persistence module

## API Layer Standards

### Error Handling

#### Standard Error Envelope
```json
{
  "success": false,
  "message": "Human readable message",
  "correlationId": "uuid",
  "details": {} // Optional, only when truly needed
}
```

#### Error Handling Strategy
| Error Type | HTTP Code | Response Strategy | Example |
|------------|-----------|-------------------|---------|
| **Validation** | 400 | Specific field errors | "Page must be >= 0" |
| **Not Found** | 404 | Entity-specific message | "Document with ID 123 not found" |
| **External API** | 502/503 | Service unavailable | "Congress API temporarily unavailable" |
| **Rate Limit** | 429 | Retry guidance | "Rate limit exceeded, retry after 60s" |
| **Unexpected** | 500 | Generic message + correlation ID | "Internal error, correlation: uuid" |

#### Exception Handling Rules
- **Controllers**: Always return error envelope (never throw to framework)
- **Services**: Let domain exceptions bubble up, log unexpected exceptions
- **External Calls**: Circuit breaker + fallback responses
- **Logging**: ERROR level only for unexpected exceptions with stack traces
- **Security**: Never expose internal details, use correlation IDs for tracing [SEC]

### Request Validation
- **Pagination**: Enforce `page >= 0`, `1 <= size <= 100` (400 error if invalid)
- **Correlation IDs**: Accept `X-Correlation-Id` header or auto-generate, propagate to outbound calls

### Health Endpoints
- `/api/health` - Basic health check
- `/api/health/{component}` - Component-specific health  
- `/api/system/health` - Full system health (includes rate & breaker snapshots, no outbound calls)

### Idempotency
- Ingestion triggers return summary (documents processed count)
- Avoid exceptions for common no-op conditions

## Frontend Standards

### Data Fetching
**React Query Only**: Use hooks like `useDocumentsQuery` with proper keys:
```typescript
['documents', page, size, sortBy, sortDir] // 5m staleTime
```
**Cache Invalidation**: Mutations must invalidate base and infinite query variants:
```typescript
['documents-infinite', ...] 
```

### Type Safety
- **No `any` types** - Always use proper TypeScript types
- **Shared Interfaces**: Derive from backend DTO shapes, store in `types/`
- **Component Structure**: Minimal container/presentation separation, no business logic in UI components

### Data Visualization  
- **Party Breakdown**: Clamp percentages (0-100%), follow existing mapper logic

## Configuration Standards

### Properties Location
- **Runtime Config**: `backend/api-rest/src/main/resources/` (only packaged module)
- **Ignored**: Root `application.properties` in aggregator (not in runtime jar)

### Database Configuration
- **Never hardcode** `spring.datasource.driver-class-name` (auto-detect Postgres vs H2)

### External Configuration
- **Typed Properties**: Use `@ConfigurationProperties` with Bean Validation
- **Safe Defaults**: Tests must start without secrets
- **Environment Variables**: Every new var → placeholder in `.env.example` [SEC]
- **Security**: No secrets in code or logs [SEC]

## Caching & Rate Limiting

### Cache Key Strategy
**Format**: `methodName_param1_param2_param3`  
**Example**: `congressBills_2024-01-01_0_50`
**Rule**: Include ALL parameters (filters, pagination, sorting) to prevent cross-pollution

### Rate Limiting
- **Adaptive Retry Suppression**: When remaining quota % < threshold → abort retries
- **Guard Compliance**: Never ignore quota guards with retry loops

### TTL Strategy  
- **Alignment**: Match TTL to upstream data volatility
- **Centralization**: Avoid scattered inline TTL constants

## Logging & Observability

### Structured Logging
- **MDC**: Always include `correlationId` 
- **External Calls**: Log at DEBUG level (method, endpoint, latency, sanitized params)
- **Security**: Never log API keys or secrets [SEC]

### Error Logging Strategy
- **Service Failures**: null/empty returns are NOT errors (expected behavior)
- **Exception Logging**: Only unexpected exceptions at ERROR level with stack traces

### Metrics Strategy
- **Current State**: Micrometer metrics removed (Aug 2025)
- **Monitoring**: Rely on structured logs + health snapshots  
- **Future**: Reintroduction requires justification (issue + ADR)

## Testing Standards

### Test Types & Strategy
| Test Type | Approach | Mocking Strategy | Coverage Target |
|-----------|----------|------------------|-----------------|
| **Unit** | MockK for ports, isolated domain logic | No Spring context unless required | Business logic, validation |
| **Integration** | TestContainers Postgres (auto-startup) | Mock only external HTTP (instability/quota risk) | End-to-end workflows |
| **Contract** | External API integration points | Real HTTP calls in CI/staging only | API compatibility |

### Test Naming & Coverage Requirements
- **Format**: `should_action_when_condition()`
- **Mandatory Coverage**: 
  - Success path (happy case)
  - Failure paths (expected errors)
  - Boundary conditions (edge cases)
- **Critical Test Cases**:
  - Pagination validation (size=0, size>100, negative page)
  - Rate limit suppression paths
  - Circuit breaker states (open/half-open/closed)
  - Cache key completeness
  - Error envelope format consistency

### Test Determinism & Performance
- **Time-dependent**: Always inject fixed `Clock` (never use `LocalDateTime.now()`)
- **Retry Testing**: Virtual time or configurable delays (never `Thread.sleep()`)
- **External Dependencies**: 
  - Mock `OllamaService` interface (no network in unit tests)
  - Use TestContainers for database (automatic lifecycle)
  - Mock WebClient for HTTP calls (use `@MockBean` in integration tests)
- **Test Isolation**: Each test must be independent (no shared state)

## Documentation Standards

### Knowledge Base Location
- **Internal Docs**: `obsidian/LegisTrack/` only (except root `README.md`)
- **ADR Format**: `ADR-XXXX-title.md` with sequential numbering
- **ADR Content**: Context, decision, consequences + priority tags `[SEC]`, `[DATA]`, etc.

### Documentation Rules
- **Version Control**: Track stable config & notes only (no personal workspace state)
- **Security**: Never store secrets, logs, or transient output [SEC]  
- **Linking**: Use `[[Note Name]]` wiki links, keep notes focused (single concept each)

## Development Decision Trees

### When Adding New Features
```
New Feature Request
├── Database Changes Needed?
│   ├── Yes → Create new Flyway migration + indexes + timestamps
│   └── No → Continue
├── External API Integration?
│   ├── Yes → WebClient + suspend + retry + jitter + cache key + correlation
│   └── No → Continue  
├── New Configuration?
│   ├── Yes → @ConfigurationProperties + Bean Validation + .env.example
│   └── No → Continue
└── Frontend Changes?
    ├── Yes → React Query hooks + proper keys + cache invalidation
    └── No → Complete
```

### Cache Key Construction Decision
```
Adding @Cacheable?
├── Identify ALL input parameters
├── Format: methodName_param1_param2_param3
├── Include: filters, pagination, sorting
└── Validate: No parameter omissions (causes cache pollution)
```

## PR Review Checklist
**Before submitting, verify**:
- [ ] Priority trade-offs explained with tags if any
- [ ] Schema changes → New Flyway file + indexes + entity timestamps  
- [ ] External HTTP → WebClient suspend + retry + jitter + complete cache key + correlation
- [ ] Cache additions → All params in key + TTL rationale
- [ ] Errors → Standardized envelope & validation
- [ ] Tests → Unit + integration + boundary cases added/updated
- [ ] Config → `.env.example` updated + typed properties + validation
- [ ] Logging → Appropriate level + no secrets
- [ ] Frontend → Correct query keys + cache invalidations
- [ ] Docs → ADR in vault if architectural decision changed

## AI Assistant Guidelines

### Task Categories & Context Hints

#### 1. Data Layer Tasks
**When to use**: Database changes, migrations, JPA work
**Files to check**: 
- `backend/persistence-jpa/src/main/resources/db/migration/` (existing migrations)
- `backend/core-domain/src/main/kotlin/` (domain models)
- `backend/persistence-jpa/src/main/kotlin/` (JPA entities)

#### 2. API Integration Tasks  
**When to use**: External API calls, HTTP clients
**Files to check**:
- `backend/external-*/src/main/kotlin/` (existing adapters)
- `backend/api-rest/src/main/resources/application.properties` (config)

#### 3. Frontend Tasks
**When to use**: React components, API calls, UI changes
**Files to check**:
- `frontend/src/hooks/` (React Query hooks)
- `frontend/src/types/` (TypeScript interfaces)
- `frontend/src/components/` (existing patterns)

#### 4. Testing Tasks
**When to use**: Adding/fixing tests
**Files to check**:
- `backend/*/src/test/kotlin/` (test patterns)
- Test naming: `should_action_when_condition()`

### Validation Checkpoints
Before implementing, AI should verify:
1. **Architecture Compliance**: Does this follow the module separation rules?
2. **Priority Alignment**: Which priority tier does this impact? Any trade-offs?
3. **Existing Patterns**: Are there similar implementations to follow?
4. **Configuration**: Are new environment variables needed?
5. **Testing**: What test cases are required (success/failure/boundary)?

## Common Anti-Patterns (REJECT if present)
- Editing existing Flyway migrations
- Using `WebClient.block()` 
- Missing parameters in cache keys
- Hardcoded model names
- Leaking JPA entities to controllers
- Unvalidated pagination parameters
- Swallowing exceptions instead of error envelopes
- Adding TypeScript `any` types
- Storing secrets in code
- Duplicating CORS or config classes  
- Unbounded retry loops ignoring adaptive suppression
- Party breakdown with negative percentages

## Quick Reference

### Development Commands
```bash
# Full Stack
docker compose up -d

# Backend Development  
./gradlew bootRun --args='--spring.profiles.active=dev'

# Testing
./gradlew test

# Frontend Development
npm ci && npm start
```

### API Commands
```bash
# Health Check
curl http://localhost:8080/api/health

# List Documents (paginated)
curl 'http://localhost:8080/api/documents?page=0&size=5'

# Manual Data Ingestion
curl -X POST 'http://localhost:8080/api/documents/ingest?fromDate=2025-01-01'

# Trigger AI Analysis
curl -X POST 'http://localhost:8080/api/documents/123/analyze'

# Check Ollama Models
curl http://localhost:11434/api/tags
```

## Troubleshooting Guide

### Common Issues & Solutions

#### Build Failures
- **Gradle issues**: Check Java version compatibility  
- **Database connection**: Verify Postgres is running (`docker compose up -d`)
- **Port conflicts**: Check if 8080 (backend) or 3000 (frontend) are in use

#### Data Ingestion Problems
- **Rate limiting**: Check `/api/system/health` for quota status
- **Circuit breaker open**: Wait for cooldown period or restart service
- **Missing data**: Verify `fromDate` parameter and Congress.gov API availability

#### AI Analysis Issues  
- **Model unavailable**: Check Ollama service (`curl http://localhost:11434/api/tags`)
- **Analysis fails**: Verify document exists and model is loaded
- **Performance**: Check prompt size and model resource usage

#### Cache Issues
- **Stale data**: Clear application cache or restart service
- **Cache misses**: Verify cache key includes all parameters
- **Memory issues**: Check cache TTL settings and size limits

### When to Ask for Confirmation
Ask before making these changes:
- Large refactors crossing module boundaries
- Altering retry/circuit breaker semantics  
- Adding new external services
- Changing error envelope structure
- Reintroducing metrics framework
- Deviating from cache key completeness rules

### Decision Guidelines
**When unsure**: Propose minimal changes with priority ladder justification  
**Default bias**: Protective priorities ([SEC]/[DATA]/[CORR]) over optimization ([PERF]/[VELO])

---
*Keep this file authoritative; expand via ADRs for architectural decisions*

## MCP Tool Usage Guidelines

### Tool Priority Order
Use structured MCP tools before shell commands for better safety and determinism:

1. **Code Analysis**: Search/read tools for context gathering (not manual file opening)
2. **File Editing**: Use patch/edit tools (prevents formatting churn vs echo/redirection) 
3. **Planning**: Todo/task tools for multi-step changes (improves traceability)
4. **Testing**: Structured test tools before falling back to `./gradlew test`
5. **Validation**: Diagnostic tools for compilation/lint issues (not terminal output scanning)
6. **Git Operations**: Structured diff/status tools (cleaner than manual `git diff`)
7. **Network Access**: Only use terminal when no internal adapter exists [SEC][DATA]

### When to Use Terminal
Only escalate to shell commands when:
- No MCP tool exists for the operation (profiling, docker logs)
- Reproducing environment-specific behavior  
- Multi-service orchestration (`docker compose`) with no higher-level wrapper

### Benefits by Priority
- **[SEC]** Reduces secret exposure risk in shell history
- **[DATA]** Atomic, traceable changes via structured patches  
- **[CORR]** Deterministic edits prevent state divergence
- **[REL]** Repeatable workflows, fewer typos
- **[PERF]** Focused JSON output vs console noise

### Usage Rules
- Always justify terminal usage with escalation reasoning
- State MCP tool findings before shell commands
- Summarize only key changes (avoid log dumps)
- Use targeted MCP tools for failure diagnosis (not retry loops)
