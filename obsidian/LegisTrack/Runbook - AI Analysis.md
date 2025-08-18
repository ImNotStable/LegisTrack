# Runbook - AI Analysis

Status: Stable (revise when prompt structure/model selection strategy changes). Architectural changes require ADR.

## Purpose
Provide deterministic AI-generated summaries/insights for a persisted legislative `Document` using a locally (or remotely) hosted Ollama model.

## Triggers
Manual: `POST /api/documents/{id}/analyze`
(No automatic trigger during ingestion to keep ingestion latency & quota separate.)

## Preconditions
1. `Document` with given id exists.
2. Ollama model configured via `app.ollama.model` is available (`OllamaService.isModelAvailable`).
3. Circuit breaker for Ollama (if implemented) is closed or half-open.

## Workflow Steps
1. Controller receives request and validates `id`.
2. Service loads `Document` domain object.
3. Precheck model availability.
4. Build deterministic prompt using prompt builder (extend existing sections instead of concatenating ad-hoc strings to maintain consistency `[CORR]`).
5. Invoke `OllamaService` to generate analysis (stream or full response depending on adapter implementation).
6. Persist `AiAnalysis` result associated with `Document` (follow existing policy: overwrite or version; ensure policy matches code base—future change requires ADR if switching strategies).
7. Return success envelope/DTO.

## Determinism Guidelines
* No random seeds unless sourced from config.
* Prompt sections ordered consistently: Context → Bill Metadata → Actions Summary → Sponsors → Requested Output Format.
* Avoid time-relative wording (use absolute dates already present in `Document`).

## Failure Modes & Handling
| Symptom | Likely Cause | Action |
|---------|--------------|--------|
| 404 Document | Invalid id | Return 404 error envelope; no retries |
| 503 Model Unavailable | Ollama not running / model missing | Attempt `isModelAvailable`; advise operator to `ollama pull <model>` |
| Timeout / network error | Ollama service overloaded | Retry limited times with backoff (respect any breaker) |
| Empty / truncated response | Upstream abort or size limit | Log ERROR with correlationId; optionally re-run once if safe |

## Operational Checks
* List available models: `curl http://localhost:11434/api/tags`.
* Pull model (outside app): `ollama pull <model>`.
* Health snapshot: `/api/system/health` (if augmented with AI component state).

## Security & Privacy
No sensitive data in prompts; only public legislative text/metadata. Do not log full prompt or response at INFO; use DEBUG with redaction if needed `[SEC]`.

## Performance Considerations
Analysis is on-demand to prevent ingestion bottlenecks `[REL][PERF]`. If queueing emerges, consider asynchronous job + status polling (future ADR candidate).

## When to Raise an ADR
* Changing persistence strategy (e.g., from overwrite to versioned history or vice versa).
* Introducing multi-model selection or fallback chain.
* Adding summarization variants (e.g., bullet vs narrative) beyond current deterministic output.

## Related Documents
* [[Architecture Overview]]
* [[Runbook - Ingestion Pipeline]]
* [[Glossary]]
* [[Troubleshooting]]
* [[ADR-0001-multi-module-architecture]]
