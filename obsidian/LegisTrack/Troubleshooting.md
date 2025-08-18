# Troubleshooting

Concise guide for common operational & developer issues. Use correlationId from error envelopes to trace logs. If a scenario implies architectural change, open an ADR.

## Legend
Tags reflect priority rationale: `[SEC]` Security | `[DATA]` Data Integrity | `[CORR]` Correctness | `[REL]` Reliability | `[PERF]` Performance.

## 1. Circuit Breaker is OPEN
**Symptoms:** Health endpoint shows breaker OPEN for Congress adapter; ingestion returns fast failures.
**Causes:** Consecutive upstream failures (network, 5xx, malformed responses).
**Actions:**
1. Check upstream reachability (curl Congress endpoint outside app).  
2. Inspect last ERROR logs with correlationId.  
3. Wait for cooldown; half-open should attempt a trial automatically.  
4. If persistent & due to data contract change, patch adapter DTOs (add new fields) `[CORR]`.

## 2. Rate Limit Exhaustion
**Symptoms:** Remaining quota % in `/api/system/health` below threshold; ingestion halts retries early.
**Actions:**
1. Confirm adaptive suppression engaged (log message at INFO/DEBUG).  
2. Delay manual ingestion; do NOT override suppression `[REL][SEC]`.  
3. Evaluate cache key coverage; missing params increases duplicate calls `[DATA]`.  
4. Consider lowering lookback window temporarily (config change) `[PERF]`.

## 3. Duplicate Documents Persisted
**Symptoms:** Multiple rows representing same legislative item.
**Causes:** Missing/incorrect DB unique constraint or identifier mapping drift.
**Actions:**
1. Identify natural key fields (e.g., Congress session + bill number).  
2. Add unique constraint via new Flyway migration (never edit existing!) `[DATA]`.  
3. Adjust upsert logic if conditional branch skipped.  
4. Backfill duplicates manually after constraint applied.

## 4. Stale Data
**Symptoms:** New upstream actions not visible after expected delay.
**Causes:** Overly long cache TTL or ingestion failures suppressed by breaker.
**Actions:**
1. Verify cache TTL central config vs expected volatility.  
2. Flush cache (restart or targeted eviction if implemented).  
3. Run manual ingestion with explicit `fromDate` just before missing action date.  
4. Check logs for repeated suppressed retries (rate limit low) `[REL]`.

## 5. AI Analysis Fails (Model Unavailable)
**Symptoms:** 503 or error envelope stating model unavailable.
**Actions:**
1. `curl http://localhost:11434/api/tags` to list models.  
2. Pull model: `ollama pull <model>` (outside app).  
3. Re-run analysis endpoint.  
4. If still failing, verify config property `app.ollama.model` matches model tag.

## 6. Flyway Migration Error on Startup
**Symptoms:** App fails during startup with Flyway exception.
**Causes:** Out-of-order version, checksum mismatch (edited existing file), or syntax error.
**Actions:**
1. Ensure you did NOT modify existing migration (rollback changes).  
2. Create new `V{next}__description.sql` file.  
3. Validate SQL syntax locally (psql / IDE).  
4. Clean local dev DB ONLY if safe (never in shared env) `[DATA]`.

## 7. Pagination Validation Failures (400)
**Symptoms:** API returns 400 for seemingly valid pagination.
**Causes:** `size` > 100 or `page` < 0 per contract.
**Actions:**
1. Adjust client request within constraints.  
2. If larger page size needed, propose ADR weighing `[PERF]` vs `[REL]`.

## 8. Missing Correlation IDs in Logs
**Symptoms:** Hard to trace request flow.
**Causes:** Missing `X-Correlation-Id` header & generation logic failing to propagate.
**Actions:**
1. Inspect filter registering correlation in `MDC`.  
2. Add header in client calls for cross-service tracing.  
3. Confirm logs include correlationId field (structured logging pattern).

## 9. Cache Key Collision
**Symptoms:** Response shape inconsistent with requested params.
**Causes:** Cache key omitted a parameter causing pollution.
**Actions:**
1. Audit `@Cacheable` expressions; ensure all params enumerated.  
2. Add missing param; include rationale `[DATA][CORR]`.  
3. Invalidate existing cache entries (restart / targeted).  
4. Add/Update test to assert distinct keys for parameter variations.

## 10. Slow Local Dev Startup
**Symptoms:** Boot takes long.
**Causes:** Downloading dependencies, container DB cold start, large migrations.
**Actions:**
1. Use Gradle daemon & configure build cache.  
2. Run Postgres locally once to warm.  
3. Disable optional AI/ingestion startup tasks via profile if supported.

## 11. Unclear Domain–Entity Mapping
**Symptoms:** Confusion modifying persistence or domain logic.
**Actions:** Consult [[Domain Model Mapping]]; if mismatch discovered, update mapping doc & add ADR if structural rework.

## 12. Need for New Metric
**Symptoms:** Gaps in observability (e.g., unknown average ingestion latency).
**Actions:**
1. Evaluate if structured logs suffice.  
2. If not, propose minimal metric & open ADR (metrics were intentionally minimized) `[REL]`.

## Related Documents
* [[Architecture Overview]]
* [[Runbook - Ingestion Pipeline]]
* [[Runbook - AI Analysis]]
* [[Domain Model Mapping]]
* [[Glossary]]
* [[ADR-0001-multi-module-architecture]]
