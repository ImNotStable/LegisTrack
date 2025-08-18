# Domain Model Mapping

Describes relationships between pure domain objects (in `core-domain`) and JPA entities (`persistence-jpa`). Maintain when adding fields or changing identifiers. Purpose: preserve domain purity & prevent leakage of persistence concerns.

## Principles
* Domain objects contain only business-relevant data & behavior; no JPA annotations.
* Entities mirror domain structure where necessary but may include persistence-only fields (surrogate IDs, audit timestamps) `[CORR]`.
* Translators (mappers) convert between domain and entity; all conversions centralized to avoid drift.
* Natural keys favored for idempotent upsert logic; surrogate primary keys (e.g., `id` bigint) used internally.

## Typical Object Pairs
| Domain Object | Entity | Notes |
|---------------|--------|-------|
| `Document` | `DocumentEntity` | Entity holds surrogate primary key plus natural unique constraints (e.g., congress + billNumber). Domain may exclude internal DB id. |
| `Action` | `ActionEntity` | Nullable `actionDate` supported; ordering preserved by date then insertion order. |
| `Sponsor` | `SponsorEntity` | Party & chamber stored; potential indexing on party for analytics queries. |
| `AiAnalysis` | `AiAnalysisEntity` | Linked by FK to `DocumentEntity`; may include model name & timestamp metadata. |

## Identifier Strategy
* Domain uses composite natural key (session + number) implicitly; service layer responsible for lookups.
* Persistence enforces unique constraint to enable upsert by detecting conflict before insert.

## Mapping Flow
1. Ingestion builds domain `Document` aggregate.
2. Persistence service attempts to locate existing `DocumentEntity` by natural key.  
3. If found: update mutable fields (e.g., new actions) – ensure append vs duplicate (set or de-dup logic).  
4. If not found: create new entity graph from domain.  
5. Save; repository returns entity; domain may not need database-generated id.

## Action De-Duplication
When re-ingesting, compare candidate action signature (type + date + optional code). If identical exists, skip to maintain idempotence `[DATA]`.

## Audit Fields
| Field | Purpose |
|-------|---------|
| `createdAt` | First persistence timestamp (DB or application assigned). |
| `updatedAt` | Last modification timestamp (updated each upsert). |

Domain objects may omit these; services can enrich DTOs if necessary for API responses.

## Adding a New Field (Checklist)
1. Add to domain object (core-domain).  
2. Add column via new Flyway migration (never edit existing).  
3. Update JPA entity & mapping functions.  
4. Extend repository queries if needed (ensure indexes).  
5. Update DTO mappers (API) & TypeScript types.  
6. Add tests (domain + persistence + controller).  
7. Update this document & any affected runbooks.  
8. If design decision non-trivial (e.g., change from natural to surrogate key in domain), create ADR.  

## Removal / Renaming
Requires new migration handling data transformation; consider deprecation period. Record rationale in ADR with `[DATA]` implications.

## Related Documents
* [[Architecture Overview]]
* [[Runbook - Ingestion Pipeline]]
* [[Glossary]]
* [[Troubleshooting]]
* [[ADR-0001-multi-module-architecture]]
