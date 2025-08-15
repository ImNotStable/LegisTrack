# LegisTrack AI Coding Assistant Instructions

## 🚀 Ultra-Concise Agent Primer (Read in <60s)
Purpose: Track & analyze US legislation. Flow = Ingest (Congress.gov) -> Persist (Postgres) -> AI Analyze (Ollama) -> Serve REST -> React UI.

Core Modules:
- Ingestion: `ScheduledDataIngestionService` calls `CongressApiService` (suspend + WebClient + retry + cache) -> build `Document` + relations (sponsors, actions) -> persist.
- AI: `OllamaService` generates `AiAnalysis` (reuse prompt builders; deterministic).
- API: `/api/documents` (list/page/detail + trigger ingestion). Errors map to `{"success": false, "message": "..."}`.
- Frontend: React Query hooks in `useApi.ts`, components under `components/` render feed/cards/detail.

Golden Rules (DO):
1. Add CHANGES.md entry for EVERY change (correct format, newest on top).
2. New entities: Kotlin data class + timestamps + Flyway V__ migration (never edit old) + needed indexes.
3. External calls: suspend + WebClient + retry (>=2s backoff) + DEBUG log + `@Cacheable` (key includes ALL params).
4. Failures: return null/empty (services) – NO thrown exceptions for normal error paths.
5. Frontend data fetching ONLY via React Query with key `['documents', page, size, sortBy, sortDir]` and 5m staleTime.
6. Keep secrets/config in `.env` + mirror placeholders in `.env.example`; never hardcode.
7. Respect priority hierarchy (SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE) when making trade‑offs.

Golden Rules (DON'T):
- Don’t modify existing Flyway migrations; create a new versioned file.
- Don’t use blocking I/O in services or class components / `any` in frontend.
- Don’t omit params in cache keys or skip CHANGES.md update.
- Don’t create new markdown files; only update allowed docs.
- Don’t expose internal exception details in API responses.

Testing Essentials:
- Backend integration = TestContainers (Postgres). Mock external HTTP cautiously; use MockK only.
- Name tests `should_doSomething_when_condition()`; cover success + failure.

Quick Commands:
- Start stack: `docker-compose up -d` (or VS Code task "Start LegisTrack System").
- Backend dev: `./gradlew bootRun`  | Tests: `./gradlew test`
- Frontend dev: `npm start` | Tests: `npm test`

When unsure: propose minimal diff citing rule refs (e.g. [DATA], [REL]). Full detailed rules remain below.

## ⚡ TL;DR (Read First)
Goal: Ship correct changes fast. Never violate REQUIRED / FORBIDDEN rules.

Core Stack:
- Backend: Spring Boot 3.4 + Kotlin 2.1 (`backend/src/main/kotlin/com/legistrack`) with PostgreSQL + Redis
- Frontend: React 18 + TypeScript + Tailwind + React Query (`frontend/src`)
- AI: Local Ollama model (`OllamaService`) analyzing ingested Congress.gov bills

Flow: Ingestion → AI → API → UI
1. `ScheduledDataIngestionService` → fetch bills (cache+retry) → persist `Document` graph
2. `OllamaService` → create `AiAnalysis`
3. `/api/documents` → list/detail JSON
4. React hooks (`useApi.ts`) → render components

Backend Additions:
- External HTTP: suspend + WebClient + retry + DEBUG log + `@Cacheable` (key: serviceMethod_param1_param2)
- New entity: data class + timestamps + new Flyway V__ migration + needed indexes
- Failures: return null/empty (no throw); controllers map to `{ "success": false, "message": "..." }`

Frontend Additions:
- Functional components only; props typed (no `any`)
- Data: React Query key `['documents', page, size, sortBy, sortDir]` (extend carefully)
- UI: Tailwind utilities; Heroicons; always loading + error states

Config & Env:
- Secrets via `.env`; template `.env.example` for new keys
- No hardcoded credentials/model names

Process:
- Every change: add `CHANGES.md` entry (category + impact + initials)
- Only modify `README.md`, `CHANGES.md`, this file for docs
- Strip dead/debug code

Gotchas:
- Never edit old Flyway files; add new
- Cache keys: include all params
- Prompts: deterministic; reuse `OllamaService` builders

If unsure: propose minimal diff + cite rule.

---

## Project Overview

LegisTrack is a U.S. legislation tracking and AI analysis system built with:
- **Backend**: Spring Boot 3.4 + Kotlin 2.1 + PostgreSQL + Redis + Ollama AI
- **Frontend**: React 18 + TypeScript + TailwindCSS + React Query
- **Infrastructure**: Docker Compose with PostgreSQL, Redis, Ollama, and Nginx

## Architecture & Key Components

### Backend (`backend/src/main/kotlin/com/legistrack/`)
- Entities: `Document`, `Sponsor`, `AiAnalysis` (+ Flyway)
- External: `CongressApiService`, `OllamaService`
- Core: `DocumentService`, `DataIngestionService`, `ScheduledDataIngestionService`
- Controllers: `/api/documents` (CORS localhost:3000)
- Config: profiles via `application*.properties`

### Frontend (`frontend/src/`)
- Components (Tailwind)
- Hooks: `useApi.ts`
- Services: `api.ts`
- Types: mirror backend DTOs
- Dev proxy: `localhost:8080`

### Data Flow
1. Ingest (cron) → persist
2. AI analysis → `AiAnalysis`
3. Cache external responses (Redis)
4. Query via paginated endpoints

### Development Patterns

#### Kotlin Conventions
- External API = `suspend`; controllers may `runBlocking`
- Entities: immutable data classes, nullable optional fields
- Services: business only; controllers = HTTP glue
- Failures: log + return null/empty

#### Config Management
- Env-specific in `application*.properties`
- Secrets via `.env` / `.env.example`
- Orchestration: `docker-compose.yml`
- Container profile: `docker`

#### Testing
- Integration: TestContainers
- Mocks: MockK
- Gradle tuned (parallel, G1GC)

## Critical Developer Workflows

### Local Development Setup
```powershell
# Start infrastructure services (Windows PowerShell)
docker-compose up -d postgres redis ollama

# Run backend (requires JDK 21)
cd backend
./gradlew.bat bootRun

# Run frontend (separate terminal)
cd frontend
npm start
```

### Key Build Commands
- **Full System**: `docker-compose up -d` (includes auto-rebuild)
- **Backend Only**: `./gradlew.bat bootRun` (Windows) or use VS Code task "Start LegisTrack System"
- **Tests**: `./gradlew.bat test` (includes TestContainers)
- **Native Build**: `./gradlew.bat nativeCompile` (GraalVM)
- **Performance Build**: Gradle optimized with G1GC, parallel forks, and maxParallelForks

### Database Operations
- **Migrations**: Flyway handles schema changes in `db/migration/V*__*.sql`
- **Manual Ingestion**: POST to `/api/documents/ingest?fromDate=2024-01-01`
- **Connection**: PostgreSQL on localhost:5432, credentials in docker-compose.yml and `.env` file
- **Init Scripts**: Database initialization via `db/init.sql` in docker-entrypoint-initdb.d
- **Init Scripts**: Database initialization via `db/init.sql` in docker-entrypoint-initdb.d

## External Dependencies & APIs

### Congress.gov (`CongressApiService`)
- Cached + retry (≥2s backoff), endpoints: bills, details, cosponsors, actions
- Auth: `CONGRESS_API_KEY`

### Ollama (`OllamaService`)
- Model: `gpt-oss:20b`
- Structured prompts (effects, economic, tags)
- Base URL env (default 11434)

### Redis
- Keys: method + params (ex: `congress-bills_2024-01-01_0_20`)
- TTL: per cache config (none explicit)
- Client: Lettuce

## Project-Specific Patterns

### Entities
- `Document` ↔ sponsors (primary flag)
- `Document` ↔ actions (history)
- `Document` ↔ analyses (versioned, valid flag)

### API Patterns
- Pagination: Spring Data `Page<T>`
- DTO split: summary vs detail
- Errors: `{ "success": false, "message": "..." }`
- Params: page,size,sortBy,sortDir

### Frontend State
- React Query (stale 5m)
- Hooks: `useDocuments`, `useTriggerDataIngestion`
- Router: simple, unknown→feed
- Styling: Tailwind + Heroicons

## Coding Rules & Guidelines

### 🚨 MANDATORY ENFORCEMENT NOTICE 🚨

**AI ASSISTANTS MUST PRIORITIZE PROJECT SUCCESS OVER CONVENIENCE - NO EXCEPTIONS**

- **PROJECT-WIDE SCOPE**: Rules apply across backend, frontend, infra, docs, tests
- **NO BYPASS**: Never ignore REQUIRED / FORBIDDEN
- **VIOLATION HANDLING**: Refuse + explain compliant path
- **PATTERN ENFORCEMENT**: Follow documented patterns strictly
- **ESCALATION**: Offer compliant alternatives (never weaken higher priority outcomes)
- **OVERRIDES**: Only direct user; confirm understanding & impact trade-offs

**Rules anchor outcome hierarchy; do not trade a higher priority (e.g. Security) for a lower one (e.g. Style).**

### Project Success Priorities (highest → lowest)
1. Security & Secret Safety [SEC]
2. Data Integrity & Consistency (DB schema, migrations) [DATA]
3. Functional Correctness (business rules, API contracts) [CORR]
4. Reliability & Resilience (retries, null-safe returns) [REL]
5. Performance & Resource Efficiency (caching, avoiding N+1) [PERF]
6. Maintainability & Clarity (structure, duplication control) [MAIN]
7. Developer Velocity (scaffolding speed) [VELO]
8. Style & Aesthetics (naming, micro-formatting) [STYLE]

Conflict Resolution:
- If CORR vs PERF: prefer correctness, then optimize.
- If REL vs VELO: add resilience first.
- Never sacrifice SEC / DATA for speed or style.
- Document any intentional PERF trade-off in `CHANGES.md` rationale.

---

### Project-Wide Standards


#### Architecture Abstraction & Flexibility
- **REQUIRED**: Favor abstractions (interfaces/ports) at service boundaries: external APIs, AI model provider, persistence queries beyond basic CRUD. [MAIN][REL]
- **PATTERN**: Use ports/adapters naming: `CongressPort` (interface) + `CongressApiService` (adapter), `AiModelPort` + `OllamaService` (adapter) when refactoring.
- **REQUIRED**: Keep domain models (`Document`, `AiAnalysis`) free of framework annotations beyond JPA—no leaking WebClient, controller, or HTTP concerns into domain.
- **REQUIRED**: New cross-cutting concerns (metrics, tracing) implemented via Spring configuration & AOP/filters—not manual scattering in services.
- **FORBIDDEN**: Hardcoding model names, URLs, or credentials inside services; always inject via configuration properties.
- **FORBIDDEN**: Creating parallel ad-hoc abstractions that duplicate existing port responsibilities—extend existing interface instead.
- **ENFORCEMENT**: Assistant must nudge toward interface-first design for new external integrations and note if a direct concrete implementation would reduce future swap ability.

#### Version & Dependency Management (All Components)
- **REQUIRED**: Declare every library, plugin, and BOM only in `gradle/libs.versions.toml`; never hardcode versions or raw coordinates in build scripts. [DATA][MAIN]
- **REQUIRED**: Use bundles for all logical groups (jackson, kotlin, http.client, db, redis, test, spring.starters); create a new domain bundle when ≥2 libs commonly co-occur.
- **REQUIRED**: New lib workflow: (1) add version (or rely on BOM), (2) add `[libraries]` entry, (3) add to existing/new bundle, (4) document in `CHANGES.md` with rationale.
- **REQUIRED**: Prioritize stability & ecosystem compatibility over novelty; prefer supported LTS, then widely adopted stable, then latest stable only if low risk, RC only for CVE/blocking issues. [REL][CORR]
- **PATTERN**: Bundle names by domain (e.g., `observability`, `ai`) to ease reuse across modules.
- **REQUIRED**: Pin exact versions (no `^`, `~`, `*`, `latest`); avoid duplicating version strings—centralize or use BOM.
- **FORBIDDEN**: Inline version overrides, raw `implementation("group:artifact:ver")`, skipping bundle when group exists, duplicating same version literal across places, using pre-release in prod.
- **ENFORCEMENT**: Assistant must refuse proposals with raw coordinates, un-bundled multi-lib adds, or destabilizing downgrades/upgrades; suggest catalog patch instead.

#### File Management & Cleanup
- **REQUIRED**: Remove all temporary files, build artifacts, and IDE-specific files from version control [MAIN]
- **REQUIRED**: Maintain comprehensive `.gitignore` with patterns for all generated content
- **FORBIDDEN**: Never commit files in `/build/`, `/target/`, `/node_modules/`, `/dist/`, or IDE folders
- **REQUIRED**: Clean up unused imports, dead code, and commented-out code blocks during development
- **REQUIRED**: Remove debug statements, console.log(), println(), and temporary test code before commits
- **PATTERN**: File organization: group related files, remove orphaned resources, maintain clear directory structure
- **REQUIRED**: Delete obsolete configuration files, deprecated dependencies, and unused assets
- **FORBIDDEN**: Never leave TODO comments without issue tracking or completion timeline
- **⚠️ ENFORCEMENT**: AI must refuse commits with useless files and actively suggest cleanup of detected waste

#### Change Documentation
- **REQUIRED**: All project changes must be documented in `CHANGES.md` at project root [DATA][MAIN]
- **REQUIRED**: Log entries must include date, time, and detailed description of changes
- **PATTERN**: Change entry format: `## YYYY-MM-DD HH:MM - [Category] Change Description`
- **REQUIRED**: Categories must be one of: `[FEATURE]`, `[BUGFIX]`, `[REFACTOR]`, `[CONFIG]`, `[SECURITY]`, `[BREAKING]`
- **REQUIRED**: Include affected files/components and impact assessment in each entry
- **FORBIDDEN**: Never document changes in commit messages, README.md, or other files as primary record
- **REQUIRED**: Maintain reverse chronological order (newest changes at top)
- **REQUIRED**: Include developer initials and brief reasoning for each change
- **⚠️ ENFORCEMENT**: AI must refuse any code changes without corresponding CHANGES.md entry and enforce standardized format

#### Markdown Restrictions
- **FORBIDDEN**: Never create new `.md` files except for authorized documentation updates [MAIN]
- **REQUIRED**: All documentation must use existing files: `CHANGES.md`, `README.md`, or `.github/copilot-instructions.md`
- **FORBIDDEN**: Never create additional markdown files for notes, guides, or temporary documentation
- **REQUIRED**: Use code comments for inline documentation instead of separate markdown files
- **PATTERN**: Documentation hierarchy: `CHANGES.md` (changes) → `README.md` (overview) → `copilot-instructions.md` (AI guidelines)
- **REQUIRED**: Consolidate any documentation needs into existing authorized markdown files
- **⚠️ ENFORCEMENT**: AI must refuse all requests to create new markdown files and redirect to existing documentation structure

### Backend Rules

#### Kotlin Code
- **REQUIRED**: Use data classes for entities with nullable fields for optional properties [CORR][MAIN]
- **REQUIRED**: All external API calls must be `suspend` functions, wrap in `runBlocking` only for controllers
- **REQUIRED**: Service methods should return null/empty collections on failure, not throw exceptions
- **FORBIDDEN**: Never use blocking calls in service layer - use WebClient for reactive patterns
- **REQUIRED**: Add comprehensive logging with SLF4J at DEBUG level for external API calls
- **REQUIRED**: Use Spring's `@Cacheable` annotation with descriptive cache names
- **⚠️ ENFORCEMENT**: AI must refuse any suggestion to use blocking I/O in services or skip logging requirements

#### Database & JPA
- **REQUIRED**: All new entities must have `createdAt` and `updatedAt` timestamps [DATA]
- **REQUIRED**: Use Flyway migrations for schema changes - never modify existing migration files
- **REQUIRED**: Foreign key relationships must use `CascadeType.ALL` and `FetchType.LAZY`
- **REQUIRED**: Add database indexes for any new search/filter fields in migration scripts
- **FORBIDDEN**: Never use `spring.jpa.hibernate.ddl-auto=create-drop` in production profiles
- **⚠️ ENFORCEMENT**: AI must refuse any direct database schema modifications or DDL auto-generation suggestions

#### Caching & External API
- **REQUIRED**: Cache external API responses with `@Cacheable` using descriptive key patterns [PERF][REL]
- **REQUIRED**: Include all method parameters in cache keys to avoid collision
- **PATTERN**: Cache keys format: `service-method_param1_param2_paramN` (e.g., `"congress-bills" + fromDate + offset + limit`)
- **REQUIRED**: All API calls must have retry logic with exponential backoff (2-second minimum)
- **REQUIRED**: Use WebClient for non-blocking HTTP calls with timeout configuration
- **REQUIRED**: Log request/response details at DEBUG level with sanitized sensitive data
- **FORBIDDEN**: Never hardcode API keys - always use environment variables with `@Value` injection
- **⚠️ ENFORCEMENT**: AI must refuse cache implementations that don't follow the key pattern or hardcoded credentials

### Frontend Rules

#### React & TypeScript
- **REQUIRED**: Use functional components with hooks - no class components [MAIN]
- **REQUIRED**: All API calls must use React Query (`@tanstack/react-query@5.17.15`) with proper error handling
- **REQUIRED**: TypeScript interfaces must match backend DTOs exactly
- **FORBIDDEN**: Never use `any` type - create proper interfaces for all data structures
- **REQUIRED**: Use exact query key patterns: `['documents', page, size, sortBy, sortDir]`
- **⚠️ ENFORCEMENT**: AI must refuse class component suggestions or `any` type usage under any circumstances

#### State & UI
- **REQUIRED**: Use React Query for server state, React Context for global UI state only [CORR][REL]
- **REQUIRED**: Configure 5-minute stale time and default retries for all queries: `staleTime: 5 * 60 * 1000`
- **PATTERN**: Query keys format: `['documents', page, size, sortBy, sortDir]`
- **REQUIRED**: Implement loading and error states for all async operations
- **REQUIRED**: Use TailwindCSS utility classes - no custom CSS except for complex animations
- **REQUIRED**: All icons must be from Heroicons library (`@heroicons/react@2.0.18`) for consistency
- **REQUIRED**: Implement responsive design for mobile, tablet, and desktop viewports
- **REQUIRED**: Add proper ARIA labels and semantic HTML for accessibility
- **⚠️ ENFORCEMENT**: AI must refuse state management that mixes server/UI state, custom CSS suggestions, or non-Heroicons icon usage

### Testing & Config Rules

#### Testing Requirements
- **REQUIRED**: Use TestContainers for integration tests with real PostgreSQL instances [CORR][DATA][REL]
- **REQUIRED**: Use MockK for mocking in Kotlin unit tests - avoid Mockito
- **REQUIRED**: Test all service methods with both success and failure scenarios
- **REQUIRED**: Use `@Transactional` for test isolation
- **REQUIRED**: Create test fixtures in separate SQL files under `test/resources`
- **FORBIDDEN**: Never rely on production data for tests
- **PATTERN**: Test method names: `should_returnExpectedResult_when_givenCondition()`
- **⚠️ ENFORCEMENT**: AI must refuse Mockito usage, tests without proper failure scenario coverage, or production data usage

#### Environment & Config
- **REQUIRED**: All environment-specific configuration must be defined in `.env` file at project root [SEC][MAIN]
- **REQUIRED**: Use `.env.example` template with placeholder values for all required variables
- **PATTERN**: Environment variables format: `UPPER_SNAKE_CASE` with Spring Boot @Value injection: `@Value("\${CONGRESS_API_KEY}")`
- **REQUIRED**: Group related variables with consistent prefixes (e.g., `DATABASE_*`, `REDIS_*`, `OLLAMA_*`)
- **FORBIDDEN**: Never commit actual `.env` files with real API keys - only `.env.example` templates
- **REQUIRED**: Document each variable's purpose and expected format in `.env.example`
- **PATTERN**: Variable naming: `{SERVICE}_{COMPONENT}_{PROPERTY}` (e.g., `DATABASE_URL`, `REDIS_HOST`)
- **REQUIRED**: Include sensible defaults in `application.properties` that reference env vars
- **REQUIRED**: All services must be defined in `docker-compose.yml` with resource limits
- **REQUIRED**: Use Alpine Linux base images for production builds (`postgres:15-alpine`, `redis:7-alpine`)
- **FORBIDDEN**: Never commit secrets or API keys to version control - use placeholder values in .env.example
- **⚠️ ENFORCEMENT**: AI must refuse any configuration that bypasses .env standardization, commits actual secrets, or configurations without resource limits

### Error Handling & Monitoring

#### Exceptions & Monitoring
- **REQUIRED**: Log all exceptions with full stack traces at ERROR level [REL][MAIN]
- **REQUIRED**: Return user-friendly error messages - never expose internal details
- **PATTERN**: API error response: `{"success": false, "message": "User-friendly message"}`
- **REQUIRED**: Use specific exception types for different failure scenarios
- **REQUIRED**: Add health check endpoints for all external dependencies
- **REQUIRED**: Use structured logging with correlation IDs for request tracing
- **REQUIRED**: Monitor external API rate limits and implement backoff strategies
- **REQUIRED**: Track cache hit/miss ratios for performance optimization
- **⚠️ ENFORCEMENT**: AI must refuse generic exceptions, responses that expose internal details, or implementations without proper monitoring

## Common Debugging Scenarios

### AI Analysis Issues
- Check Ollama service health: `curl http://localhost:11434/api/tags`
- Verify model availability in `OllamaService.isModelAvailable()`
- Review analysis prompts in `buildGeneralEffectPrompt()` methods

### Data Ingestion Problems
- Monitor scheduled job logs: `ScheduledDataIngestionService`
- Test API connectivity: Congress.gov endpoints in `CongressApiService`
- Check database constraints: bill_id uniqueness, foreign key relationships

### Performance Optimization
- Enable SQL logging: `spring.jpa.show-sql=true`
- Monitor Redis hit rates: cache key patterns in service methods
- Review database indexes: defined in `V1__initial_schema.sql`

## 📌 Open Issues Alignment (Do Not Reintroduce)
Reference `issues.md` for full list; key actionable constraints for assistants:
1. Coroutines: Eliminate `WebClient.block()` inside `suspend` flows; prefer Kotlin coroutine extensions (e.g., `awaitBody`). Avoid unnecessary `runBlocking` in controllers—convert to `suspend` endpoints if refactoring.
2. External APIs: Fix parameter omissions (e.g., Congress members `chamber`), replace manual Map parsing with typed DTOs/Jackson, correct GovInfo granule path composition, tighten `isModelAvailable` matching (exact model name).
3. Caching: Centralize cache specs (TTL + names) in `CacheConfig`; maintain structured keys (current rule requires all params) but consider keyGenerator only if it preserves parameter uniqueness.
4. Config Duplication: Remove duplicate CORS registrations & redundant Gradle/detekt or Foojay resolver duplications in future refactors; keep single authoritative definition.
5. DTO/Data Integrity: Allow null for uncertain `actionDate` fields; replace placeholder text retrieval (`getGovInfoText`).
6. Frontend Query Invalidation: When adding mutations, also invalidate infinite query key variant (e.g., `['documents-infinite', ...]`).
7. Party Breakdown: Guard against negative third bar (clamp at 0) due to rounding.
8. Resilience: Introduce jittered exponential backoff + page/limit validation (clamp excessive values) when modifying request handlers.
9. Logging & Security: Avoid DEBUG verbosity in production profiles; ensure secrets never appear in logs. Lean toward INFO for routine ingestion.
10. Testing Gaps: Add integration tests around external Congress/GovInfo parsing before large parser refactors.

When implementing changes touching these areas, reference both this section and `issues.md`; never add code that worsens listed problems (e.g., new `block()`, additional duplicate config, unchecked large limits).
