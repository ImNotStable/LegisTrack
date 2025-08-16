# Modularization & Architecture Evolution Plan

> Living document guiding the incremental extraction of a modular backend (multi-module Gradle) and feature-sliced frontend. Keep concise commit-oriented updates. Use `[REFACTOR]`, `[FEATURE]`, `[BREAKING]` tags in CHANGES.md referencing sections below.

## Executive Summary

This document contains three major improvement initiatives for LegisTrack:

1. **🏗️ Backend Modularization** (Phases 0-10): Transform monolithic Spring Boot app into multi-module Gradle project with clean domain boundaries, ports/adapters pattern, and 35%+ build time reduction target.

2. **⚡ Document Loading Optimization** (Phases DL-1 to DL-9): Eliminate N+1 queries, add caching, and reduce API response times by 30%+ through batch fetching and pagination improvements.

3. **🐳 Docker Runtime Stability** (Phases DR-1 to DR-7): Fix Congress API 403 errors, Ollama model loading issues, and improve container startup reliability with proper health checks.

**Current Status**: All phases are in planning stage (⬜ Not Started). Priority: Complete Phase 0 baseline capture first.

**Overall Progress**: 
- 🏗️ Modularization: 1/11 phases complete (9%)
- ⚡ Document Loading: 0/9 phases complete (0%)  
- 🐳 Docker Runtime: 0/7 phases complete (0%)

## Table of Contents
1. [Objectives & Success Criteria](#1-objectives--success-criteria)
2. [Proposed Backend Module Layout](#2-proposed-backend-module-layout)
3. [Ports & Contracts](#3-ports--contracts)
4. [Frontend Feature-Sliced Structure](#4-frontend-feature-sliced-structure)
5. [Migration Phases (Incremental)](#5-migration-phases-incremental)
6. [Arch Rules & Static Analysis](#6-arch-rules--static-analysis)
7. [Testing Strategy](#7-testing-strategy)
8. [Data & Schema Strategy](#8-data--schema-strategy)
9. [Build & CI Changes](#9-build--ci-changes)
10. [Risks & Mitigations](#10-risks--mitigations)
11. [Immediate Next Actions (Phase 0)](#11-immediate-next-actions-phase-0)
12. [Phase Completion Checklist Templates](#12-phase-completion-checklist-templates)
13. [Backlog / Deferred Ideas](#13-backlog--deferred-ideas)
14. [Governance](#14-governance)
15. [Document Loading Improvement Plan](#document-loading-improvement-plan)
16. [Docker Runtime Stability Plan](#docker-runtime-stability-plan)

Visual diagrams (Mermaid + ASCII) have been added to `README.md` (Architecture section) to avoid duplication here while keeping this file focused on actionable steps.
Plaintext / unicode quick-reference diagrams (dependency matrix, timeline, data flow) also reside in the README under "Plaintext / Unicode Diagrams".
Additional detailed box-drawing diagrams (layered backend, request lifecycle, AI pipeline, frontend composition, CI flow, organization analogy) are in the README under "Box-Drawing Diagrams".

## 1. Objectives & Success Criteria

Prioritized objectives (SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE):

1. Enforce clean domain boundaries (no JPA/persistence leakage) `[CORR][DATA]`
2. Reduce build & test cycle time ≥35% `[VELO][REL]`
3. Pluggable external integrations via ports (Congress, Ollama) `[REL][MAIN]`
4. Isolate ingestion from API exposure (independent deploy/build triggers) `[MAIN][PERF]`
5. Separate AI analysis lifecycle & prompts from persistence `[CORR]`
6. Layered, faster, clearer tests (unit vs integration) `[REL]`
7. Centralize caching, retry, metrics config `[PERF][MAIN]`
8. Eliminate accidental cross-module imports (ArchUnit=0 violations) `[CORR]`

KPIs / Metrics:
- Full backend build (cold) time: BASELINE t0 = <record> → Target t1 ≤ 0.65 * t0
- Average targeted module test run time (core-domain + ingestion) ≤ 25% of monolith full test time
- ArchUnit violations: baseline N → 0 by Phase 9
- Adapter swap ease: Replace AI model adapter with mock in <5 LOC changes
- Domain branching coverage ≥70%; DB integration tests isolated to persistence-jpa & api-rest slices
- Cache hit ratio improvement vs baseline (log-derived)

## 2. Proposed Backend Module Layout

```
root
├── core-domain                (pure Kotlin domain models, value objects, domain services, events)
├── persistence-jpa            (JPA entities, Spring Data repos, Flyway migrations, mappers)
├── ingestion                  (ingestion orchestrators, schedulers, pipelines using ports)
├── ai-analysis                (prompt builders, AI orchestration, analysis creation)
├── external-congress-adapter  (implements CongressPort via WebClient/HTTP)
├── external-ollama-adapter    (implements AiModelPort)
├── common-infra               (logging, caching, metrics, retry, Jackson, security)
├── api-rest                   (Spring Boot main app, controllers, DTOs, model mappers)
└── testing-support            (test fixtures, builders, TestContainers mgmt)
```

Dependency Direction (must remain acyclic):
```
core-domain <- persistence-jpa (entities map to domain)
core-domain <- ingestion, ai-analysis
core-domain <- external-* adapters (implement ports)
common-infra is depended on by adapter + api-rest (no reverse)
api-rest depends on domain + feature modules + adapters (no one depends on api-rest)
testing-support depends on core-domain (others use it in test scope only)
```

Rules:
- Only persistence-jpa contains JPA annotations.
- api-rest is the only executable boot module (Spring Boot main class lives here).
- Ports (interfaces) defined close to domain (core-domain) unless adapter-specific.

## 3. Ports & Contracts

Interfaces (initial draft):
```kotlin
interface CongressPort {
	suspend fun fetchBills(updatedSince: Instant, page: Int, pageSize: Int): List<BillSummary>
	suspend fun fetchBillDetail(id: BillId): BillDetail?
}

interface AiModelPort {
	suspend fun generateAnalysis(prompt: String, temperature: Double = 0.2): AnalysisResult
}

interface DocumentRepositoryPort {
	fun save(document: Document): Document
	fun findById(id: DocumentId): Document?
	fun search(criteria: DocumentSearchCriteria): List<Document>
}
```

Domain Events (sealed): `DocumentIngested`, `AnalysisCreated` (Phase 5 consider outbox pattern).

Versioning: Internal semantic version tags in KDoc (`@since 0.2-mod`). Breaking changes documented.

## 4. Frontend Feature-Sliced Structure

```
src/
	app/ (providers, routing, QueryClient)
	shared/{ui,api,lib,config}
	entities/{document,analysis}
	features/
		documents/{api,components,model}
		ingestion/{api,components}
		analysis/{api,components}
	pages/{DocumentsPage,DocumentDetailPage,...}
	widgets/{DocumentFeedWidget,...}
	processes/ (future multi-step flows)
```

Conventions:
- Each feature owns React Query hooks (query & mutation keys co-located).
- Entities layer defines canonical TS types + normalizers.
- Avoid deep relative paths via barrel files (`index.ts`).

## 5. Migration Phases (Incremental)

| Phase | Focus | Key Actions | Exit Criteria | Status |
|-------|-------|-------------|---------------|--------|
| 0 | Prep Baseline | Record build/test metrics; introduce ports (interfaces only); add ArchUnit baseline (ignored) | Metrics captured; ports compile | ✅ Complete |
| 1 | Extract core-domain | Create module; move pure models & domain services; add mapping stubs | No Spring deps in core-domain | ⬜ Not Started |
| 2 | Extract persistence-jpa | Move entities/repositories + Flyway; implement repository ports; add mappers | All persistence tests green | ⬜ Not Started |
| 3 | External adapters | Split Congress & Ollama adapters; centralize HTTP config in common-infra | Ports implemented; old services removed | ⬜ Not Started |
| 4 | Ingestion module | Move ingestion orchestration & schedulers; replace direct repo calls with ports | Ingestion tests pass; api still functional | ⬜ Not Started |
| 5 | AI analysis module | Move AI orchestration & prompt code | AI features still green | ⬜ Not Started |
| 6 | api-rest separation | Move controllers & main app; others library modules | App boots via api-rest | ⬜ Not Started |
| 7 | common-infra | Introduce infra module; deduplicate config | No duplicate cache/retry config | ⬜ Not Started |
| 8 | Testing realignment | Introduce testing-support; refactor tests per layer | Faster selective test runs | ⬜ Not Started |
| 9 | Harden boundaries | Turn on ArchUnit & Detekt rules (fail build) | 0 violations | ⬜ Not Started |
| 10 | Cleanup & docs | Remove deprecated code; finalize docs | README & CHANGES updated | ⬜ Not Started |

## 6. Arch Rules & Static Analysis

ArchUnit (Phases 0→9):
- Forbid access to `..persistence..entity..` from outside persistence-jpa.
- Layers: domain, adapters, application (ingestion/ai), interface (api-rest).
- Detect cycles among packages & modules.

Detekt custom rules:
- Ban imports of `org.springframework.data.jpa` in non-persistence modules.
- Flag direct use of `WebClient` outside adapter modules.

## 7. Testing Strategy

Layers:
- Unit: core-domain (no Spring).  Fast.
- Contract: Ports vs fakes (CongressPort expectations, AiModelPort prompt shape).
- Integration: persistence-jpa with TestContainers (singleton container). Adapter HTTP using WireMock/MockWebServer.
- Service/Slice: ingestion & ai-analysis with stubbed ports.
- API: api-rest slice + limited end-to-end (happy path ingestion → analysis retrieval).

Coverage Targets: domain branch ≥70%, persistence critical repos ≥80% line, adapter retry logic 2+ failure scenarios.

## 8. Data & Schema Strategy

- Flyway stays in persistence-jpa (`resources/db/migration`).
- Entities <-> Domain mapping explicit (manual or MapStruct later). No exposing entities beyond persistence.
- Transaction boundaries only in application/service layer (api-rest, ingestion, ai-analysis) not in domain.
- Potential future: outbox table for events (Phase >10 optional).

## 9. Build & CI Changes

Gradle:
- Add modules in `settings.gradle.kts` incrementally.
- Enable configuration on demand & parallel (verify compatibility).
- Use version catalogs for shared deps (already present `libs.versions.toml`).
- Separate test tasks: `:core-domain:test`, etc. CI matrix can run in parallel.
- Add `testFixtures` for testing-support consumers.
- Build cache & remote cache (optional later) for reproducibility.

CI Pipeline Stages (future refinement):
1. Validate (lint, detekt, archunit baseline) – fast
2. Unit & contract tests (core-domain + ports)
3. Integration tests (persistence, adapters) in parallel
4. API tests & smoke run
5. Publish artifacts / Docker image (api-rest only)

## 10. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Circular deps | Guard with dependency constraints + ArchUnit cycle rule |
| Mapping overhead | Benchmark; inline or generate mappers if hotspot |
| Test flakiness (containers) | Reuse singleton containers + retry analyzer tests |
| Partial migration confusion | Document phases; keep this TODO updated |
| Scope creep | Stick to phase exit criteria; defer extras to backlog |

## 11. Immediate Next Actions (Phase 0)

### Phase 0 Progress: ✅ Complete

- [x] Record current backend full build time (cold & warm)
- [x] Add initial ports (CongressPort, AiModelPort, DocumentRepositoryPort) inside existing module
- [x] Add ArchUnit dependency & baseline test (ignore failures for now)
- [ ] Create CHANGES.md entry `[REFACTOR] Begin modularization Phase 0`
- [ ] Commit baseline metrics in this section

### 11.1 Baseline Capture Procedure

**Commands to run:**
```bash
# 1. Cold build (after clean)
./gradlew clean build -x test

# 2. Warm build (no changes)  
./gradlew build -x test

# 3. Full test suite
./gradlew test

# 4. Frontend build
npm run build
```

**Baseline Metrics (captured):**
```
Date: 2025-08-15
t_build_cold = 1.1s (assemble only, skipping lint/AOT)
t_build_warm = 1.0s (assemble up-to-date)
t_test_single = 1.0s (ArchUnit baseline test)
t_frontend_build = 7.3s (npm run build)
archunit_violations = 0 (baseline test passes, enforcement rules commented out)
modules_count = 1 (monolithic structure)
```

### 11.2 Port & Mapping Conventions (Apply starting Phase 0)

- Port interface naming: `<Capability>Port` (e.g., `CongressPort`, `AiModelPort`).
- Repository ports aggregate by aggregate root (`DocumentRepositoryPort`). Avoid leaking persistence technology.
- Mappers: `XxxMapper` with functions `toDomain(entity)` and `toEntity(domain)`; keep pure, no side effects.
- Collections: always return immutable (`List`, not mutable) from ports.
- Nullability: prefer nullable return (`T?`) over empty sentinel objects.
- Caching annotations: only in adapter implementations, not ports or domain.

### 11.3 Definition of Done per Phase

| Phase | Definition of Done Additions |
|-------|------------------------------|
| 0 | Ports compile; baseline metrics recorded; ArchUnit dependency added with placeholder test (ignored) |
| 1 | No Spring imports in `core-domain`; all domain invariants moved; temporary mappers in original module pass tests |
| 2 | All JPA entities & repositories relocated; new module builds; existing features unaffected; mappers green |
| 3 | External services only referenced via ports in business modules; old direct service classes deleted |
| 4 | Ingestion scheduling isolated; API still triggers ingestion manually; tests cover failure path (Congress error) |
| 5 | AI analysis creation encapsulated without direct repository calls from adapters |
| 6 | Application boot class only in `api-rest`; other modules have no `spring.factories` side effects |
| 7 | Single cache/retry config; duplicate config classes removed; property binding unaffected |
| 8 | Test tasks can run module-selectively; CI matrix splitting documented |
| 9 | ArchUnit violations = 0; Detekt custom rules active; build fails on new boundary breach |
| 10 | Stale code removed; README reflects final module layout; TODO marked with all phases completed |

### 11.4 Incremental Test Matrix

| Phase | New / Adapted Tests |
|-------|---------------------|
| 0 | ArchUnit placeholder, baseline build script timing script (manual) |
| 1 | Domain unit tests for invariants moved (ensure no dependency on Spring) |
| 2 | Repository integration tests adjusted for new package; mapping round-trip tests |
| 3 | Adapter contract tests (retry: success after transient failure) + caching key uniqueness test |
| 4 | Ingestion service failure fallback (returns partial results) |
| 5 | AI analysis prompt deterministic test (stable snapshot) |
| 6 | Smoke API test ensures controllers wired with ports (mock injection) |
| 7 | Cache configuration test (all expected caches present) |
| 8 | Test fixture reuse metrics (ensure single TestContainers spin-up) |
| 9 | ArchUnit enforcement tests (no JPA outside persistence) |
| 10 | Regression coverage review (ensure no orphaned tests) |

### 11.5 Rollback & Contingency Triggers

Rollback if any of:
- Critical endpoint 5xx rate increases >10% after module extraction.
- Build time regression >20% sustained for 2 consecutive phases (compared to baseline).
- Test flakiness (same test failing non-deterministically >2 times in a week) traced to modular change.

Rollback strategy:
1. Revert latest phase commit series (tag phases when complete: `phase-X-complete`).
2. Open issue documenting root cause before reattempting.
3. Apply mitigation (e.g., reduce module boundary granularity) then retry phase.

### 11.6 Naming & Packaging Rules

- Kotlin package root per module: `com.legistrack.<module>`; domain stays `com.legistrack.domain`.
- Avoid cyclic sub-packages; keep `adapter`, `port`, `service`, `config` suffix naming consistent.
- Feature flags (if introduced) placed in `common-infra.flags` package.

### 11.7 Quick Start Execution Order

**Phase 0 → 1 Checklist:**
1. ✅ Capture baseline metrics
2. ✅ Add port interfaces  
3. ✅ Add ArchUnit dependency
4. ✅ Update CHANGES.md
5. ✅ Commit & tag `phase-0-start`
6. ⬜ Create `core-domain` module
7. ⬜ Move domain value objects
8. ⬜ Add mappers & update tests
9. ⬜ Complete domain model migration
10. ⬜ Tag `phase-1-complete`

### 11.8 Metrics Update Template

```
### Metrics Snapshot (Phase X)
Date: YYYY-MM-DD
Build (cold/warm): t_build_cold = __s / t_build_warm = __s
Tests full: t_test_full = __s
ArchUnit violations: __
Notes: <observations>
```

## 12. Phase Completion Checklist Templates

Template (copy for each phase):
```
### Phase X Completion
Date: YYYY-MM-DD
Commit(s): <hashes>
Exit Criteria Status:
- [x] Criterion 1
- [x] Criterion 2
Metrics Delta:
	Build time: t0 -> tX
	Violations: N0 -> NX
Notes:
	<brief>
```

## 13. Backlog / Deferred Ideas

- Outbox/event sourcing for ingestion events
- MapStruct adoption for mapper generation
- GraphQL API gateway module (future) separate from REST
- Feature toggles module (lightweight) for selective rollout
- Remote build cache (Gradle Enterprise or local caching server)

## 14. Governance

- PRs touching more than one module need justification in description.
- New external calls require defining a port first (no direct WebClient in business modules).
- ArchUnit & Detekt must pass locally before merge (after Phase 9 activation).

---
Maintain this file as the single source for modularization progress. Update checklists as phases advance.

---

# Document Loading Improvement Plan

Goal: Reduce perceived and actual latency when listing, paginating, and viewing document details while preserving correctness and security rules. Target ≤ 500ms P95 list page generation (backend) and < 150ms React hydration after data available (frontend) on dev hardware.

## A. Baseline & Metrics

Metrics to capture before changes:
- Backend: `/api/documents?page=0&size=20` response time (avg, P95) over 30 requests (warm JVM)
- DB: Query count & total time for one page (use Hibernate statistics / p6spy or log)
- Frontend: Time to first list render (network start → first paint with data) using browser devtools
- Cache: Redis hit/miss for any cached lookups (if introduced later)

Targets:
- Reduce DB queries per page from current (est. 1 + 2n) to ≤ 3 total (page + sponsors batch + analyses batch) without N+1.
- Shrink backend P95 for list page by ≥30% vs baseline.
- Reduce frontend re-renders of list container to 1 per pagination action.

## B. Current Pain Points (Inferred)

1. N+1 style pattern: For each document, separate queries for sponsors and analyses (2n extra queries).
2. Repeated per-document analysis selection when only first valid needed.
3. No server-level projection optimizing summary payload (fetching full Document entity fields not needed for list? acceptable but can project).
4. Missing prefetch/prefetch caching of next page on frontend (user experiences gap when paging quickly).
5. Lack of conditional request / ETag; list always refetched despite 5m staleTime when sorting unchanged.
6. Potential absence of proper DB indexes on `introduction_date`, `bill_id`, `industry_tags` (need to verify migration later).
7. Large page sizes up to 100 could amplify N+1 cost.
8. Backend sorting flexible but unvalidated field names—risk of non-index scan (plan to whitelist).

## C. Strategy Overview (Phased)

| Phase | Focus | Key Change | KPI |
|-------|-------|-----------|-----|
| DL-1 | Metrics & Observability | Enable Hibernate statistics or p6spy; measure baseline query count/time | Baseline recorded |
| DL-2 | Batch Fetch Optimization | Replace per-document sponsor/analysis queries with IN-clause batch retrieval | Queries/page ≤3 |
| DL-3 | Summary Projection | Introduce lightweight summary JPA projection (id, billId, title, date, status, latestValidAnalysisTagSubset) | Payload shrink (%) |
| DL-4 | Backend Caching | Cache summary page results for short TTL (e.g., 30s) keyed by params | Cache hit ratio ≥50% during rapid nav |
| DL-5 | Sorting Whitelist & Index Validation | Enforce allowed sort fields; add missing indexes via Flyway | No full table scan in EXPLAIN |
| DL-6 | Frontend Prefetch & Infinite Scroll Option | Prefetch next page when user scrolls 70% down or on hover of pagination | Perceived latency ↓ |
| DL-7 | Optimistic Detail Enhancement | Prefetch document detail on card focus/hover using queryClient.prefetchQuery | Detail TTFB ↓ |
| DL-8 | ETag / If-None-Match Support | Add ETag to list responses; frontend conditional fetch (optional) | 304 ratio tracked |
| DL-9 | Final Optimization & Cleanup | Remove deprecated code paths, finalize docs | KPIs met |

## D. Backend Detailed Actions

DL-1:
- Add `spring.jpa.properties.hibernate.generate_statistics=true` in dev profile OR integrate p6spy (ensure no secrets logged [SEC]).
- Provide Logback logger for `org.hibernate.stat` at DEBUG.

DL-2:
- Add repository methods: `findSponsorsByDocumentIds(ids: List<Long>)`, `findAnalysesByDocumentIds(ids: List<Long>)` returning grouped results.
- In service: collect page doc IDs, batch fetch map by docId, assemble DTOs (O(n) mapping).
- Ensure analyses query filters only `isValid = true` and picks earliest or latest (define requirement: choose latest valid by createdAt/analysisDate).

DL-3:
- Introduce interface projection `DocumentSummaryProjection` with required scalar fields only.
- Replace `findAllWithValidAnalyses` with `findAllSummary(pageable)` plus separate aggregated analysis tags (if needed) via batch.
- Optional: store latest valid analysis id on `documents` table for O(1) join (out-of-scope now; evaluate after map optimization).

DL-4:
- Use `@Cacheable("documents-page")` with key = page_size_sortField_sortDir (includes all params) storing list + metadata wrapper (not Page object to avoid serialization overhead; reconstruct PageImpl).
- TTL configured in cache config (30s) balancing staleness/performance. Avoid caching if page > 50 (rare).

DL-5:
- Whitelist sort fields: `introductionDate`, `title`, `status`.
- Validate request param; fallback to default if invalid.
- Flyway migration to add missing indexes (if absent): `CREATE INDEX IF NOT EXISTS idx_documents_intro_date ON documents(introduction_date DESC);` and index on lower(title) if used for sorting, plus GIN index on `ai_analyses(industry_tags)` if query frequency high.

DL-6 (Frontend):
- Introduce `usePrefetchNextDocuments` hook: watch list viewport intersection or scroll position → prefetch next page query key.
- Optionally convert to infinite scroll experience (already have `useInfiniteDocuments` hook; integrate in UI toggle) ensuring accessibility (retain buttons).

DL-7 (Frontend detail prefetch):
- On `DocumentCard` mouseenter/focus: `queryClient.prefetchQuery(['document', id], fetcher)` with low staleTime.
- Debounce hover to 150ms to avoid thrash.

DL-8:
- Add ETag header: hash of (page doc IDs + latestUpdatedAt among them).
- Frontend: add conditional request using `If-None-Match` (fetch wrapper enhancement); on 304 reuse cached data.

DL-9:
- Remove old per-document fetch methods if unused.
- Update README performance notes & TODO plan closure.

## E. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Cache staleness shows outdated analysis tags | Short TTL + manual invalidation on analysis mutation |
| Batch queries produce large IN clause | Limit page size ≤100; use chunking if size > 500 (not expected) |
| Additional indexes slow writes | Measure ingestion time; only add necessary indexes (explain analyze) |
| Prefetch increases bandwidth | Gate by user interaction & abort fetch if route changes |
| ETag hash expensive | Use precomputed `updated_at` max and join doc IDs string; hash once |

## F. Definition of Done (Document Loading Initiative)

- Baseline metrics recorded & added to CHANGES.md entry.
- Query count per list page ≤3 post DL-2.
- Batch mapping logic covered by unit + integration tests.
- Frontend prefetch behind feature flag (simple boolean) initially.
- Cache hit ratio logged (INFO) when enabled.
- README updated with improvements summary at end.

## G. Immediate Next Steps (DL-1)

### Document Loading Phase DL-1 Progress: ⬜ Not Started

- [ ] Enable Hibernate stats in dev profile.
- [ ] Add measurement script/notes capturing baseline.
- [ ] CHANGES.md entry `[REFACTOR] Start document loading optimization (DL-1)`.
- [ ] Prepare repository batch query signatures (stub, not used yet).


---

# Docker Runtime Stability Plan

Goal: Ensure reliable, reproducible local and CI startup of all services (PostgreSQL, Redis, Ollama, Backend, Frontend) with deterministic model availability and external API resilience. Address observed failures (Congress API 403, Ollama premature close, repeated container shutdown sequences).

## A. Observed Issues (from latest logs)
1. Congress API 403 Forbidden on `GET https://api.data.gov/congress/v3/bill` (likely missing required path parameters or invalid/blocked API key configuration). Response handled via exception stack traces (noise) instead of graceful empty return path earlier in lifecycle.
2. Ollama model pull failures: `PrematureCloseException` during `POST /api/pull` despite HTTP 200, indicating streaming body closed early (network timeout, container shutdown, or concurrent shutdown triggered by compose stop).
3. Repeated fast shutdown of services within ~2 minutes (frontend nginx SIGQUIT, backend graceful shutdown, database & redis SIGTERM) suggests `docker compose down` or an orchestrator stop rather than internal crashes—baseline still needs stable long-lived run for metrics.
4. Lack of explicit health-gating sequence for backend depending on Ollama model download completion (backend attempts pull then logs failure before shutdown sequence), no retry/jitter on model bootstrap beyond initial attempt.
5. Potential over-verbosity: large repeated reactive stack traces for expected external failures (403). This increases log noise and obscures actionable errors.
6. Security concern: Local `.env` file appears present with concrete `CONGRESS_API_KEY`; ensure it is not committed (verify untracked) and sanitize logs (already partially implemented) while avoiding accidental leakage in stack traces.

## B. Root Cause Hypotheses
| Issue | Hypothesis |
|-------|------------|
| 403 Congress API | Incorrect endpoint invocation without required parameters, invalid key, or missing `fromDateTime` semantics for listing; possibly rate-limited / missing Accept header. |
| Ollama premature close | Stream closed due to container stop before model finished downloading (compose down) or network timeout > keepAlive; large model size causing exceeded keep-alive window. |
| Repeated shutdowns | Manual or scripted `docker compose down` during capture; not an intrinsic crash. |
| Model bootstrap failure noise | Lack of retry with backoff/jitter for model pull + no idempotent existence check prior to pull. |
| Log noise | Exceptions not downgraded to INFO/WARN with concise message for handled external failures. |
| Potential secret exposure | `.env` tracked risk; need automated check in CI to assert `.env` not in Git index. |

## C. Remediation Phases
| Phase | Code / Config Focus | Actions | Exit Criteria |
|-------|---------------------|---------|---------------|
| DR-1 | Baseline & Guardrails | Confirm `.env` untracked; add CI check script; add README note for model pre-pull optional; record current startup durations. | `.env` not tracked; baseline times captured. |
| DR-2 | Congress API Handling | Add graceful 403 handling returning empty with single WARN (no stack trace); consider lightweight HEAD or ping endpoint for health; validate required params. | No stack trace spam on 403; ingestion continues. |
| DR-3 | Ollama Bootstrap Robustness | Implement existence check (`/api/tags` exact match) before pull; add retry with exponential backoff + jitter for pull; abort after N attempts but keep app alive (defer analysis). | Model eventually available or app logs degraded mode without crash. |
| DR-4 | Startup Sequencing | Add backend startup listener: delay scheduled ingestion until Ollama model readiness OR timeout fallback; optionally use Spring `ApplicationRunner` with coroutine retry. | Ingestion skips model-dependent analysis until ready. |
| DR-5 | Health & Monitoring | Add lightweight health indicators: Congress API (simple quick GET with timeout), Ollama model loaded flag, DB migration status; expose composite readiness. | `/actuator/health` shows subcomponents with UP/DOWN accurately. |
| DR-6 | Logging Hygiene | Convert predictable external errors (403, 404) to WARN with sanitized URI; keep ERROR only for unrecoverable (5xx, repeated premature close after retries). | Log scan shows reduced stack traces for handled cases. |
| DR-7 | Documentation & Cleanup | Update README (operations section) with model pre-pull command and troubleshooting table; consolidate any duplicate config; finalize TODO closure. | Docs updated; stale bootstrap code removed. |

## D. Detailed Actions
DR-1:
- Add a simple script/CI step ensuring `git ls-files .env` empty.
- Record startup times: time from `docker compose up -d` to backend health UP.

DR-2:
- In `CongressApiService`, catch `WebClientResponseException.Forbidden` and log WARN once with sanitized info; return empty response object (already returns empty but demote severity/remove stack trace for 403).
- Optionally add `Accept: application/json` header if API requires explicit content negotiation.

DR-3:
- Add function `isModelPresent(model: String): Boolean` querying `/api/tags` and matching exact model name.
- Wrap model pull in retry (`3 attempts`, backoff 5s, jitter 0.3). On failure, mark a `modelDegraded=true` flag exposed via health indicator.

DR-4:
- Introduce a `ModelReadinessService` storing readiness state. Scheduled ingestion checks this state before attempting analysis generation.
- Add timeout (e.g., 2 minutes) after which ingestion proceeds without analysis, logging that analysis will be backfilled once model ready.

DR-5:
- Implement custom `HealthIndicator` beans: `congressApi`, `ollamaModel`, `redisCache` (if not already), each respecting short (≤1s) timeouts.
- Aggregate readiness across indicators for gating external traffic if desired (future).

DR-6:
- Introduce logging utility to map `WebClientResponseException` status classes to log level.
- Ensure sanitized URIs (already in place) used consistently; avoid printing API key query param.

DR-7:
- Update README under operations/troubleshooting with: pre-pull command `docker exec legistrack-ollama ollama pull gpt-oss:20b` (optional), interpreting health indicator statuses, and fallback behavior when model not ready.
- Remove any obsolete bootstrap flags once stable.

## E. Metrics & KPIs
- Model readiness time (seconds) from backend container start to model loaded or degraded flag set.
- Congress API failure rate (403 occurrences per ingestion cycle) after DR-2.
- Log noise reduction: count stack trace lines containing `WebClientResponseException$Forbidden` before vs after (target ≥80% reduction).
- Startup latency improvement (baseline vs post DR-4) – aim ≤ +5% overhead after adding checks.

## F. Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Added retries delay startup | Cap retry duration and run asynchronously after app becomes live. |
| Health checks cause rate-limit | Use HEAD/light endpoints, cache results for short period. |
| Over-suppressed logs hide real issues | Only downgrade specific known status codes (403/404); keep correlation IDs for tracing. |
| Model pull still fails mid-stream | Provide manual remediation instructions in README + surface degraded state via health. |

## G. Definition of Done
- No repetitive stack traces for known 4xx external responses.
- Backend remains UP even if model pull fails; health shows `ollamaModel: DOWN` until resolved.
- Recorded metrics show model readiness attempts & outcomes.
- README updated with troubleshooting guidance.
- CHANGES.md contains entry for plan introduction (this phase). 

## H. Immediate Next Steps (DR-1)

### Docker Runtime Phase DR-1 Progress: ⬜ Not Started

- [ ] Verify `.env` untracked (already appears ignored).
- [ ] Add CI script placeholder (not yet implemented) to fail if `.env` tracked.
- [ ] Measure baseline startup duration & log counts.
- [ ] Add CHANGES.md entry `[REFACTOR] Add Docker Runtime Stability Plan`.


