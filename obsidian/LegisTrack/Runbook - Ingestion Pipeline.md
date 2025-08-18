# Runbook - Ingestion Pipeline

Status: Stable (update if retry/circuit breaker semantics or schema mapping changes). For architectural shifts create an ADR.

## Purpose
Acquire recent legislative data from Congress.gov, normalize into domain aggregates, persist idempotently, and expose via REST. Fast, reliable ingestion under variable rate limits and upstream reliability constraints.

## Triggers
* Scheduled cron (configured in Spring, see `ScheduledDataIngestionService`).
* Manual: `POST /api/documents/ingest?fromDate=YYYY-MM-DD` (optional explicit fromDate). Returns summary count.

## Date Window Logic
If `fromDate` absent, compute: `fromDate = (today(Clock) - lookbackDays)`. Lookback is a configuration property. Use injected `Clock` for deterministic tests.

## Functional Steps
1. Determine `fromDate`.
2. Page through Congress API (size configured) via external adapter.
3. For each page: parse response DTOs → construct domain `Document` (actions, sponsors). Null `actionDate` allowed.
4. Persist using persistence service (upsert). Unique constraints on natural identifiers prevent duplicates.
5. Emit count summary. Do NOT trigger AI analysis automatically.

## Key Components
* `CongressApiService` (adapter): suspend `WebClient`, exponential backoff with jitter.
* `ScheduledDataIngestionService` (ingestion module): orchestrates periodic runs.
* `Document` aggregate (core-domain): central domain object.
* JPA repositories & entities (persistence-jpa): translate to persistence layer.

## Resilience & Rate Management
| Concern | Mechanism |
|---------|-----------|
| Transient failures | Exponential backoff (≥2s base) + jitter |
| Consecutive hard failures | Circuit breaker trips after threshold |
| Low remaining rate quota | Adaptive retry suppression below threshold (default 10%) |
| Duplicate data | Idempotent upsert + unique DB constraints |
| Upstream latency spikes | Backoff increases + caching reduces repeat calls |

## Caching
Outbound Congress calls use `@Cacheable`. Key must enumerate all input params (pattern `method_param1_param2...`). TTL centrally configured. Cache reduces pressure and shields rate limits. Reject PRs missing full key `[DATA][CORR]`.

## Observability
* Health endpoints: `/api/system/health` includes rate & breaker snapshot.
* Logs: DEBUG for outbound calls (endpoint, latency, sanitized params); ERROR only for unexpected exceptions.
* Correlation: `X-Correlation-Id` accepted/propagated.

## Common Failure Modes & Responses
| Symptom | Likely Cause | Action |
|---------|--------------|--------|
| 429 / rate-limit header low | Quota nearly exhausted | Allow adaptive suppression to halt retries; reschedule later |
| Circuit breaker OPEN | Recent consecutive failures | Investigate upstream status; after cooldown half-open test auto-runs |
| Stale data | Excessive cache TTL | Review central TTL config; adjust & flush cache region |
| Duplicate rows | Missing constraint/regression | Add unique constraint via new Flyway migration (never edit existing) |
| Slow ingestion | Large lookback window or low page size | Tune lookbackDays & page size; confirm backoff not stuck at max |

## Safety / Priority Justification
* Backoff & suppression protect quotas and stability `[SEC][REL]`.
* Idempotent persistence ensures data correctness `[DATA][CORR]`.

## Manual Operations
1. Trigger manual ingestion (sample): `curl -X POST 'http://localhost:8080/api/documents/ingest?fromDate=2025-01-01'`.
2. Check health: `curl http://localhost:8080/api/system/health` (review breaker state & remaining rate %).
3. Investigate failure: search logs for correlationId from error envelope.

## When to Raise an ADR
* Changing breaker thresholds globally.
* Introducing new external data source (e.g., GovInfo).
* Altering cache TTL strategy (per-endpoint differentiation beyond central config).

## Related Docs
* [[Architecture Overview]]
* [[Troubleshooting]]
* [[Glossary]]
* [[Domain Model Mapping]]
* [[Runbook - AI Analysis]]
