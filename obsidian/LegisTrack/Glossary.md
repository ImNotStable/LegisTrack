# Glossary
Core terms used across the codebase and docs. Prefer referencing these definitions in comments & ADRs.

| Term | Definition |
|------|------------|
| Document | Domain aggregate representing a legislative item (bill/resolution) including metadata, list of Actions, Sponsors, and related attributes. |
| Action | A recorded event in the lifecycle of a Document (e.g., introduced, referred to committee). May have nullable actionDate if upstream missing. |
| Sponsor | Legislator sponsoring the Document. Data includes party & chamber where applicable. |
| Ingestion | Process fetching upstream data and persisting normalized domain objects (see [[Runbook - Ingestion Pipeline]]). |
| Idempotent Upsert | Persistence pattern ensuring repeated ingestion of same upstream record does not create duplicates—merged or ignored based on unique key. |
| Circuit Breaker | Resilience pattern tracking consecutive failures; opens to prevent continual failing calls. Exposed via health endpoints. |
| Adaptive Retry Suppression | Logic halting further retries when remaining rate quota % below threshold to preserve availability. |
| Rate Quota % | (Remaining API requests / Total quota) * 100 as provided or inferred from upstream headers. |
| TTL (Cache) | Time-to-live for cached outbound Congress responses; centrally configured. |
| Correlation ID | UUID tracing a single request path across components and logs. Provided by client or generated server-side. |
| AiAnalysis | Stored AI-generated summary/insight object linked to a Document (see [[Runbook - AI Analysis]]). |
| Prompt Builder | Deterministic construction of AI prompt sections; extended rather than ad-hoc concatenated. |
| Flyway Migration | Versioned SQL script (`V{n}__description.sql`) altering DB schema (never edit existing). |
| ADR | Architecture Decision Record documenting context, decision, consequences (e.g., [[ADR-0001-multi-module-architecture]]). |
| Priority Ladder | Ordered decision rubric: SEC > DATA > CORR > REL > PERF > MAIN > VELO > STYLE. |

## Related Documents
* [[Architecture Overview]]
* [[Runbook - Ingestion Pipeline]]
* [[Runbook - AI Analysis]]
* [[Troubleshooting]]
* [[Domain Model Mapping]]
