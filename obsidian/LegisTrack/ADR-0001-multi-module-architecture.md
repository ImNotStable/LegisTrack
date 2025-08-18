# ADR-0001 Multi-Module Architecture

Date: 2025-08-17
Status: Accepted
Tags: [SEC][DATA][CORR][REL]

## Context
LegisTrack ingests external legislative data, applies domain normalization, persists records, and offers optional AI analysis. Requirements include: strong separation of concerns, testability (especially for domain logic), resilience against upstream instability, and controlled propagation of external concerns (HTTP, JPA) to limit coupling and ease substitution.

Monolithic layering risks:
* Domain objects polluted with persistence/web annotations.  
* Harder unit testing (need Spring context).  
* Coupled retry & caching logic with business rules.  
* Increased surface for accidental data contract leaks `[DATA][CORR]`.

## Decision
Adopt a multi-module Gradle structure:
* `core-domain` – pure Kotlin domain model & service ports (no Spring, no persistence, no HTTP types).
* `external-congress-adapter` – outbound adapter: Congress API client (suspend WebClient, retry, caching, circuit breaker awareness).
* `ingestion` – orchestrates data ingestion scheduling & normalization.
* `persistence-jpa` – JPA entities, repositories, Flyway migrations (schema evolution). Domain <-> entity mapping layer.
* `ai-analysis` – AI prompt construction and model interaction logic (domain oriented, relies on abstracted `OllamaService`).
* `external-ollama-adapter` – concrete Ollama HTTP integration.
* `api-rest` – Spring Boot application exposing REST controllers, DTO mapping, error handling, correlation injection.
* `frontend` – React UI consuming REST API with React Query.

## Rationale
* Purity & Testability: Domain isolated, enabling fast unit tests without Spring `[CORR]`.
* Reliability & Resilience: External integrations separated, enabling targeted retry/circuit breaker configuration `[REL]`.
* Data Integrity: Prevents accidental persistence annotation drift into domain `[DATA]`.
* Security: Narrower module responsibility reduces likelihood of leaking secrets in logs across layers `[SEC]`.
* Evolution: Each concern can evolve independently (e.g., swapping Ollama adapter) with minimal cascade.

## Alternatives Considered
1. Single module monolith: Simpler initial setup but sacrifices clarity & test granularity; higher regression risk.  
2. Two-module (domain+application): Insufficient to isolate distinct external adapters and AI concerns, leading to bloated application module.  
3. Service mesh / microservices: Overhead unjustified for current scope; complexity would threaten higher priority tiers `[SEC][DATA]`.

## Consequences
Positive:
* Clear ownership boundaries; easier onboarding (map requirement to module quickly).
* Faster builds for targeted changes (Gradle incremental compilation per module).
* Encourages explicit contracts via ports.

Negative / Trade-offs:
* Slight initial overhead creating modules & inter-module dependencies.
* Cross-cutting changes (e.g., adding field) require touching multiple modules (domain, persistence, DTO, TS types) but enforced discipline benefits `[DATA]`.

## Implementation Notes
* Shared versions managed in Gradle version catalog.
* Only `api-rest` packaged for runtime (fat jar). Root `application.properties` is ignored.
* Domain modules must not depend on Spring or JPA.

## Metrics / Validation
* Unit test time reduced (domain tests under N seconds typical).  
* No domain class contains JPA imports (CI static analysis candidate).  
* Adapter boundaries enforce caching key completeness (code review checklist item).

## Future Considerations
* Potential additional module for search/indexing if feature scope expands (add ADR).
* Evaluate if AI analysis moves async; may justify separate processing module.

## References
* [[Architecture Overview]]
* [[Domain Model Mapping]]
* [[Runbook - Ingestion Pipeline]]
* Priority Ladder (root contributor guide)
