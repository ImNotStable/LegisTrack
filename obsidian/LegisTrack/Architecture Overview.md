# Architecture Overview

Status: Living Document (update alongside significant architectural changes; for decisions create/append ADRs like [[ADR-0001-multi-module-architecture]]).

## Goal
LegisTrack automates acquisition, normalization, persistence, and analysis of U.S. legislative data (initially Congress.gov) and exposes it through a REST API and React UI, with optional AI summaries.

## Priority Ladder
Referenced throughout: SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE. Higher tiers are never sacrificed for lower ones. When a trade‑off is made, tag it (e.g., `[DATA]`).

## High-Level Flow
```
Congress.gov API -> External Adapter -> Ingestion Services -> Domain Model -> Persistence (Postgres, JPA) -> REST DTOs -> React Frontend -> User
                                                     \-> AI Analysis (Ollama) -> Stored AiAnalysis linked to Document
```

## Modules
* `core-domain`: Pure Kotlin domain objects & ports (no Spring / JPA). Defines aggregates like `Document` (with actions, sponsors) and service interfaces.
* `external-congress-adapter`: Implements outbound port for Congress API using suspend `WebClient`, retry, caching, circuit breaker, rate tracking.
* `ingestion`: Orchestrates scheduled & manual ingestion (`ScheduledDataIngestionService`). Transforms adapter DTOs into domain aggregates.
* `persistence-jpa`: JPA entities + repositories + Flyway migrations. Mapping layer isolates domain from persistence (no leakage of JPA types outward).
* `ai-analysis`: Prompt builders and Ollama integration (model name via config). Produces deterministic `AiAnalysis` for a `Document`.
* `external-ollama-adapter`: Handles model availability & generation via Ollama HTTP API.
* `api-rest`: Spring Boot application (fat jar) exposing controllers, DTO mappers, error envelope, health endpoints, correlation handling.
* `frontend`: React + TypeScript + React Query powered UI.

## Separation of Concerns
* Domain purity enforced by excluding Spring/JPA dependencies in `core-domain`.
* Adapters isolate external IO (Congress, Ollama) so domain logic is testable and deterministic.
* Persistence is an implementation detail; controllers speak DTOs, services speak domain objects.

## Ingestion Pipeline Summary
See [[Runbook - Ingestion Pipeline]] for operational detail.
1. Trigger (cron or POST `/api/documents/ingest`).
2. Determine `fromDate` (configurable lookback) using injected `Clock` for determinism.
3. Fetch paged remote data with retry+backoff+jitter while respecting adaptive rate suppression.
4. Normalize into domain `Document` aggregate.
5. Idempotent upsert into Postgres via JPA repositories (unique constraints ensure no duplicates).
6. (Optional) Separate analysis trigger for AI.

## AI Analysis Workflow
Detailed in [[Runbook - AI Analysis]]. Triggered on-demand to avoid slowing ingestion & to respect model resource usage. Validates model availability, builds deterministic prompt, persists result.

## Circuit Breaker & Rate Awareness
External Congress adapter tracks consecutive failures. Breaker states surfaced via `/api/system/health` and component endpoints. Remaining rate quota influences retry decisions (adaptive suppression when below threshold to prioritize availability over fresh completeness `[REL][DATA]`).

## Caching Strategy
Cache keys include every input parameter (pattern: `method_param1_param2_...`). TTLs centralized (avoid scattered magic numbers). Prevents stale cross-contamination `[DATA][CORR]`.

## Error Handling & Observability
* Standard error envelope: `{ success:false, message, correlationId, details? }`.
* Correlation IDs accepted from `X-Correlation-Id` or generated; propagated outbound via `WebClient` filter.
* Structured logs with MDC correlationId; DEBUG for outbound call details; ERROR only for unexpected exceptions.

## Testing Strategy (Summary)
* Unit tests use mocks for ports; domain logic isolated.
* Integration tests with TestContainers Postgres; external HTTP boundaries mocked.
* Deterministic time via injected `Clock`.
* Boundary tests for pagination, rate limit suppression, breaker states.

## Repository Tooling & Conventions
* Single unified `.editorconfig` governs indentation & line length (Kotlin 120, general 100) `[CORR]`.
* Ktlint enforces Kotlin style; Spotless enforces license headers & trailing whitespace rules (ratcheted from `origin/main` to avoid churn) `[CORR][REL]`.
* CI workflow runs: build, ktlint, spotless, tests, and dependency update reports on PRs.
* Dependency update visibility: Gradle Versions Plugin (`dependencyUpdatesFiltered`) + `npm run dep:outdated`.
* License: MIT; header template applied automatically to new Kotlin / Gradle files.
* Automated dependency maintenance via Renovate (`.github/renovate.json5`): grouped PRs (kotlin-stack, spring-platform), weekly schedule (before 7am Monday UTC), automerge safe npm patch/minor & GitHub Action updates. `[REL][VELO]`

## Frontend Data Access
React Query hooks with stable keys, 5m stale time. Mutations invalidate list & infinite variants. No direct fetch in components. See [[Glossary]] for term definitions like "Document".

## Future Directions (Candidate ADRs)
* Expand ingestion to GovInfo.
* Introduce lightweight metrics (with ADR justification) if observability gaps appear.
* Versioned AI analysis history retention policy.

## Related Documents
* [[Runbook - Ingestion Pipeline]]
* [[Runbook - AI Analysis]]
* [[Troubleshooting]]
* [[Domain Model Mapping]]
* [[Glossary]]
* [[ADR-0001-multi-module-architecture]]
* [[ADR-TEMPLATE]]
