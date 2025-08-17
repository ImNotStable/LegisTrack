# LegisTrack AI Coding Assistant Instructions

## 🚀 Ultra-Concise Agent Primer (Read in <60s)
Purpose: Track & analyze US legislation. Flow = Ingest (Congress.gov) -> Persist (Postgres) -> AI Analyze (Ollama) -> Serve REST -> React UI.

Core Modules:
- Ingestion: `ScheduledDataIngestionService` calls `CongressApiService` (suspend + WebClient + retry + cache) -> build `Document` + relations (sponsors, actions) -> persist.
- AI: `OllamaService` generates `AiAnalysis` (reuse prompt builders; deterministic).
- API: `/api/documents` (list/page/detail + trigger ingestion). Errors map to `{"success": false, "message": "..."}`.
- Frontend: React Query hooks in `useApi.ts`, components under `components/` render feed/cards/detail.

Golden Rules (DO):
2. New entities: Kotlin data class + timestamps + Flyway V__ migration (never edit old) + needed indexes.
3. External calls: suspend + WebClient + retry (>=2s backoff) + DEBUG log + `@Cacheable` (key includes ALL params).
4. Failures: return null/empty (services) – NO thrown exceptions for normal error paths.
5. Frontend data fetching ONLY via React Query with key `['documents', page, size, sortBy, sortDir]` and 5m staleTime.
6. Keep secrets/config in `.env` + mirror placeholders in `.env.example`; never hardcode.
7. Respect priority hierarchy (SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE) when making trade‑offs.

Golden Rules (DON'T):
- Don’t modify existing Flyway migrations; create a new versioned file.
- Don’t use blocking I/O in services or class components / `any` in frontend.
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
## LegisTrack – Concise AI Assistant Guide (Operational)

Purpose: Track & analyze US legislation. Pipeline: Congress.gov ingest → Postgres persist → Ollama AI analysis → REST API → React UI.

Core Modules (dirs):
1. `ingestion/` fetches & normalizes bills via `CongressApiService` (suspend + WebClient + retry + @Cacheable).
2. `ai-analysis/` builds deterministic prompts & calls `OllamaService` (model configurable, no hardcoded names).
3. `persistence-jpa/` JPA entities (`Document`, `Sponsor`, `AiAnalysis`) + Flyway migrations.
4. `backend` (api-rest monolith layer) exposes `/api/documents` (list/detail/ingest/analyze) returning DTO + standard error envelope `{"success": false, "message": "..."}`.
5. `frontend/src` React + TypeScript + Tailwind; data via React Query hooks (see `hooks/`).

Golden Priority Order: SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE. Never trade higher for lower.

Non‑Negotiable Rules:
1. Never edit existing Flyway files; new schema change = new `V__` migration with indexes + `createdAt` & `updatedAt` on entities.
2. All external HTTP = suspend WebClient + exponential retry (≥2s base) + DEBUG log + `@Cacheable` key including ALL params (`method_param1_param2`). No blocking calls.
3. Service failure paths return null/empty; controllers translate to error envelope (no internal stack traces to clients).
4. Cache keys & env vars: no hardcoded secrets/model names. Add env var + `.env.example` placeholder.
5. React data access ONLY through React Query with key `['documents', page, size, sortBy, sortDir]` (5m staleTime); no `any`, only functional components.
6. Dependency versions only in `gradle/libs.versions.toml`; add library = version entry → library alias → bundle.
7. Test style: MockK for unit; TestContainers Postgres for integration; method names `should_doThing_when_condition()` covering success & failure.
8. New external integration = define port interface then adapter (no leaking HTTP concerns into core-domain).
9. Logging: DEBUG for external call details (sanitize), ERROR with stack trace for exceptions, never log secrets.
10. No new markdown docs; update this file or `README.md` only.

Key Workflows:
- Start stack: `docker-compose up -d` (or VS Code task). Backend dev: `./gradlew bootRun`. Tests: `./gradlew test`. Frontend: `npm install && npm start`.
- Manual ingestion trigger: POST `/api/documents/ingest?fromDate=YYYY-MM-DD`. Analysis trigger: POST `/api/documents/{id}/analyze` (returns updated detail DTO when available).
- Add entity: data class + migration + repository port + mapper update + tests.

Patterns & Examples:
- Cache key example: `congressBills_2024-01-01_0_50`. Include every param; otherwise collision risk.
- Error response example (404 / validation): `{"success": false, "message": "Document not found"}`.
- Party breakdown logic clamps & scales percentages (see `DocumentMapper`). Avoid introducing negative/overflow values.

Common Pitfalls to Avoid:
- Editing historic migrations; omitting param in cache key; blocking `WebClient.block()`; adding `any` types; leaking exceptions; hardcoding model name (`gpt-oss:20b`) instead of config.

When Unsure: propose minimal diff referencing rule tag (e.g. [DATA], [REL]) and wait for confirmation if trade‑off crosses priority boundaries.

Open Issues Watchlist (do not regress): eliminate blocking calls; typed DTO parsing for Congress; centralized cache config; accurate party breakdown; input param validation; avoid duplicate CORS/config blocks.

Provide: rationale + impacted modules + test adjustments in each PR/diff suggestion.

End of concise guide — keep it lean; expand only if a rule cannot be applied with current info.
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
