## 2025-08-15 21:00 - [REFACTOR] Complete Phase 1: Extract core-domain module

Files changed:
- backend/settings.gradle.kts (added core-domain module)
- backend/build.gradle.kts (added core-domain dependency)
- backend/core-domain/ (new module with domain-only dependencies)
- backend/core-domain/src/main/kotlin/com/legistrack/domain/entity/ (pure domain entities without JPA)
- backend/core-domain/src/main/kotlin/com/legistrack/domain/dto/ (domain DTOs)
- backend/core-domain/src/main/kotlin/com/legistrack/domain/service/ (domain service interface and implementation)
- backend/core-domain/src/main/kotlin/com/legistrack/domain/mapper/ (entity-DTO mapping utilities)
- backend/core-domain/src/main/kotlin/com/legistrack/domain/annotation/ (domain annotations)
- TODO.md (updated Phase 1 status and progress tracking)

Description:
Successfully extracted the core-domain module as the first step in modularizing the monolithic backend. Created pure domain entities (Document, Sponsor, DocumentSponsor, DocumentAction, AiAnalysis) without JPA annotations or Spring dependencies. Implemented domain service interface for business logic, mapping utilities for entity-DTO conversion, and comprehensive DTOs for API responses. The module has zero Spring dependencies (only Kotlin stdlib and Jackson), establishing a clean domain layer foundation.

Impact assessment:
- Establishes clean separation between domain logic and infrastructure concerns ([ARCH][MAIN]).
- Enables future persistence adapter extraction in Phase 2 without domain coupling.
- Domain entities use Long references instead of JPA entity relationships for cleaner boundaries.
- DocumentDomainService provides reusable business logic validation and processing.
- Mapping utilities enable consistent entity-DTO conversion patterns.
- Multi-module Gradle structure supports selective compilation and testing.

Phase 1 success criteria met:
✓ Core-domain module created with independent build configuration
✓ Pure domain entities moved without persistence annotations
✓ Domain DTOs and services implemented with business logic
✓ Zero Spring dependencies in core-domain (verified via dependency analysis)
✓ Main module successfully depends on and builds with core-domain
✓ All existing tests pass with new modular structure

Developer initials: AI, JH

## 2025-08-15 20:35 - [REFACTOR] Complete Phase 0: Baseline capture & port interfaces

Files changed:
- backend/gradle/libs.versions.toml (added ArchUnit version 1.3.0)
- backend/build.gradle.kts (added ArchUnit test dependency)
- backend/src/main/kotlin/com/legistrack/port/ (new package with CongressPort, AiModelPort, DocumentRepositoryPort interfaces)
- backend/src/test/kotlin/com/legistrack/ArchitectureTest.kt (baseline test with future rules commented out)
- TODO.md (updated baseline metrics, marked Phase 0 complete, updated progress indicators)

Description:
Completed Phase 0 of the modularization plan by establishing baseline performance metrics and introducing port interfaces. Captured build timings (1.1s cold/1.0s warm backend, 7.3s frontend), added ArchUnit dependency with baseline test for future enforcement, and extracted port interfaces from existing services without breaking changes. Port interfaces (CongressPort, AiModelPort, DocumentRepositoryPort) define contracts for external dependencies and repository operations, enabling future adapter extraction in subsequent phases.

Impact assessment:
- Establishes measurable baseline for 35% build time reduction target ([PERF][REL]).
- Introduces architectural boundaries preparation without runtime changes ([CORR][MAIN]).
- ArchUnit baseline test provides violation detection framework for Phase 9 enforcement.
- Port interfaces enable dependency inversion pattern for upcoming modular extraction.
- Zero breaking changes; existing services unchanged, interfaces additive only.

Phase 0 success criteria met:
✓ Baseline metrics captured and documented in TODO.md
✓ Port interfaces compile and integrate cleanly
✓ ArchUnit dependency added with passing baseline test
✓ No existing functionality affected

Developer initials: AI, JH

## 2025-08-15 19:55 - [REFACTOR] Add architecture diagrams & link from TODO
## 2025-08-15 19:58 - [REFACTOR] Add plaintext/unicode architecture diagrams
## 2025-08-15 20:02 - [REFACTOR] Add box-drawing architecture diagrams
## 2025-08-15 20:05 - [REFACTOR] Refine modularization execution plan (TODO.md)
## 2025-08-15 20:10 - [REFACTOR] Add document loading improvement plan
## 2025-08-16 01:15 - [REFACTOR] Add Docker Runtime Stability Plan (TODO.md)

Files changed:
- TODO.md (added Docker Runtime Stability Plan section: observed issues, root cause hypotheses, remediation phases DR-1→DR-7, actions, metrics, risks, DoD, immediate next steps)

Description:
Introduced structured plan to remediate runtime startup issues surfaced in compose logs: Congress API 403 responses, Ollama model pull premature close, lack of model readiness gating, noisy repeated stack traces, and need for health indicators. Plan sequences improvements (baseline guardrails, graceful external error handling, resilient model bootstrap with retries & degraded mode, startup sequencing, health & monitoring, logging hygiene, documentation) without implementing code yet—keeping changes low risk and documentation-only.

Impact assessment:
- Enhances reliability & resilience roadmap ([REL]) by codifying phased actions (DR-1→DR-7).
- Supports security & maintainability ([SEC][MAIN]) via secret tracking guardrails and reduced log noise.
- No runtime changes yet; safe preparatory documentation aligning with existing priorities.

Developer initials: AI, JH

Files changed:
- TODO.md (added Document Loading Improvement Plan section: metrics, phases DL-1→DL-9, backend optimizations, frontend prefetch strategy, risks, definition of done, immediate next steps)

Description:
Introduced a structured, phased optimization plan targeting backend query reduction (N+1 removal via batch fetch), payload minimization, caching, and frontend prefetch/ETag strategies to decrease document list latency and perceived load time. Aligns with performance & reliability priorities while documenting rollback and risk mitigations within existing markdown (no new files).

Impact assessment:
- Establishes clear KPIs (query count, P95 latency) improving performance focus ([PERF][REL]).
- Reduces future ambiguity for implementation sequencing, supporting maintainability ([MAIN]).
- No code changes yet; safe documentation-only addition.

Developer initials: AI, JH


Files changed:
- TODO.md (added baseline capture procedure, port/mapping conventions, per-phase definitions of done, incremental test matrix, rollback triggers, naming/packaging rules, quick start operational checklist, metrics snapshot template)

Description:
Expanded actionable guidance for upcoming modularization to reduce ambiguity during phased extraction. Introduces measurable definitions of done, standardized naming and mapping practices, rollback triggers with thresholds, and a test matrix keyed to each phase. Enhances predictability and lowers coordination overhead while staying within existing documentation files (no new markdown created).

Impact assessment:
- Improves correctness and reliability ([CORR][REL]) by formalizing exit criteria and rollback conditions.
- Enhances velocity ([VELO]) through clarified quick start steps and consistent naming.
- No runtime changes; documentation-only, low risk.

Developer initials: AI, JH


Files changed:
- README.md (added Box-Drawing Diagrams section: layered architecture, request lifecycle, AI pipeline, frontend composition, build & CI flow, organization analogy)
- TODO.md (extended reference to new box-drawing diagrams section)

Description:
Expanded architecture documentation with Unicode box-drawing diagrams to provide higher-fidelity structural views that complement Mermaid and plaintext representations. These support quick cognition in terminals and code review tools lacking Mermaid rendering while maintaining single authoritative location (README) per documentation consolidation rules.

Impact assessment:
- Improves maintainability and onboarding speed ([MAIN][VELO]) via multiple appropriately-scoped visual abstractions.
- No runtime or build impact.
- Reinforces modular boundary intentions before implementation phases commence.

Developer initials: AI, JH


Files changed:
- README.md (added Plaintext / Unicode Diagrams section: dependency matrix, phase timeline, data flow, ingestion sequence, caching & retry placement, frontend composition tree)
- TODO.md (extended reference note to include plaintext/unicode diagrams)

Description:
Supplemented Mermaid visuals with monospace pipe/unicode diagrams for quick reference during code reviews where Mermaid rendering may not be available (e.g., terminal viewers, diff tools). Maintains single source in README; TODO kept action-oriented while linking to diagrams per documentation consolidation rules.

Impact assessment:
- Improves accessibility of system architecture in environments lacking Mermaid support ([MAIN][VELO]).
- Zero runtime effect; documentation only.
- Reinforces dependency and phase clarity without duplicating logic.

Developer initials: AI, JH


Files changed:
- README.md (added Mermaid diagrams: high-level flow, dependency direction, ingestion sequence, frontend feature slices, ASCII snapshot)
- TODO.md (added reference note pointing to visuals in README)

Description:
Introduced visual architecture representations to improve shared understanding of planned modularization (multi-module backend + feature-sliced frontend) without duplicating narrative content in multiple markdown files. Diagrams clarify dependency direction constraints, ingestion flow, and upcoming frontend feature boundaries. `TODO.md` remains action-focused, deferring visuals to canonical README per documentation consolidation rules.

Impact assessment:
- Enhances maintainability and onboarding speed ([MAIN][VELO]) via clear system mental model.
- No runtime or build changes; purely documentation.
- Reinforces intended module dependency acyclicity before refactor implementation phases.

Developer initials: AI, JH

## 2025-08-15 19:25 - [BUGFIX] Commit Gradle wrapper JAR & refine ignore rules

Files changed:
- .gitignore (reordered patterns so un-ignore for wrapper jar precedes backend jar exclusion; added explanatory comment)
- backend/gradle/wrapper/gradle-wrapper.jar (added to VCS for reproducible builds)

Description:
Gradle wrapper JAR wasn't tracked because broad `backend/**/*.jar` ignore pattern overrode earlier un-ignore lines. Adjusted ordering so explicit `!backend/gradle/wrapper/gradle-wrapper.jar` follows the broad ignores, re-including the wrapper while still excluding build output jars. Added wrapper JAR to repository to guarantee consistent Gradle version resolution in CI/CD and container builds per reproducibility and velocity guidelines without requiring pre-installed Gradle. No runtime code changes.

Impact assessment:
- Ensures portable, reproducible builds across environments (containers, CI) ([CORR][REL][MAIN]).
- Prevents future build failures if Gradle version mismatch occurs externally.
- Maintains exclusion of other generated jars to keep repo lean.

Developer initials: AI, JH

## 2025-08-15 19:20 - [BUGFIX] Backend docker build fixes (settings plugin resolution & duplicate imports)

Files changed:
- backend/settings.gradle.kts (replace version catalog plugin reference with explicit Foojay plugin version 0.8.0)
- backend/src/main/kotlin/com/legistrack/controller/LegislativeApiController.kt (remove duplicate Spring imports causing ambiguity)

Description:
Resolved backend Docker image build failures after frontend fixes. Initial failure due to `settings.gradle.kts` referencing `libs.plugins.foojay.resolver.convention` during settings evaluation inside container before version catalog was available, leading to `Unresolved reference: libs`. Applied pragmatic exception (documented) by specifying the Foojay resolver plugin with explicit version `0.8.0` directly in `settings.gradle.kts` (avoids blocking build while retaining catalog usage elsewhere). Subsequent compilation failure stemmed from duplicated Spring Web annotation imports in `LegislativeApiController` producing ambiguous import errors for `RestController`, `GetMapping`, `RequestParam`, `PathVariable`, and `ResponseEntity`. Cleaned up imports to unique set.

Impact assessment:
- Restores successful backend Gradle build within Docker, unblocking full `docker compose build` pipeline.
- Minor, contained policy exception (inline plugin version) justified to restore build; future improvement could reintroduce catalog indirection via settings plugin management if needed.
- Eliminates redundant imports improving maintainability ([MAIN]) without functional changes.

Developer initials: AI, JH

## 2025-08-15 19:05 - [BUGFIX] Fix docker compose build failure (frontend npm ci lockfile mismatch)

Files changed:
- frontend/package-lock.json (regenerated to sync with pinned exact versions in package.json)
- frontend/.dockerignore (added to reduce build context size)

Description:
Addressed frontend image build failure during `npm ci` caused by stale lockfile containing caret-ranged and older dependency versions (e.g. typescript 4.9.5) conflicting with newly pinned exact versions in `package.json` (e.g. typescript 5.4.5, tailwindcss 3.3.6). Regenerated `package-lock.json` after removing outdated file to restore manifest-lock consistency, enabling deterministic Docker builds. Added `.dockerignore` to exclude `node_modules`, build artifacts, VCS, and IDE metadata from Docker build context for performance and to avoid accidental inclusion. Aligns with reproducible build and performance guidance ([CORR][PERF][MAIN]).

Impact assessment:
- Restores successful `docker compose build` (frontend stage) by eliminating lockfile mismatch.
- Ensures deterministic dependency graph matching security & stability rules; reduces future drift risk.
- Smaller Docker build context improves build performance and reduces potential leakage of local-only files.

Developer initials: AI, JH

## 2025-08-15 19:07 - [BUGFIX] Frontend dependency compatibility & lint build fix

Files changed:
- frontend/package.json (align TypeScript 4.9.5 + msw 1.3.2 with react-scripts 5 peerOptional constraint)
- frontend/package-lock.json (regenerated)
- frontend/src/components/DocumentFeed.tsx (removed unused vars; added aria-label for accessibility)

Description:
Resolved continuing Docker build failures after lockfile regeneration caused by peer dependency conflicts: `react-scripts@5.0.1` declares optional peer `typescript ^3.2.1 || ^4`, making TS 5.x incompatible. Initial attempt to force newer TS led to `npm ci` ERESOLVE errors. Downgraded to last 4.x-compatible TypeScript (4.9.5) and reverted `msw` to 1.3.2 to satisfy its peer range (<=5.2.x previously) while retaining functionality. Addressed subsequent CI build lint failure (unused variables and missing accessible name) by removing unused imports/destructured values and adding `aria-label` to sort select. Ensures reproducible, successful frontend image builds within container environment without disabling peer dependency integrity checks, aligning with correctness and reliability priorities ([CORR][REL][MAIN]).

Impact assessment:
- Restores successful `npm ci` and production build in Docker.
- Keeps dependency graph within supported peer ranges to avoid latent runtime issues.
- Improves accessibility compliance (ARIA label) and eliminates lint warnings that halted CI build.

Developer initials: AI, JH

## 2025-08-15 19:42 - [BUGFIX] Resolve multiple open issues across backend and frontend

Files changed:
- backend/src/main/kotlin/com/legistrack/config/CacheConfig.kt (stable String Redis key generator)
- backend/src/main/kotlin/com/legistrack/service/external/CongressApiService.kt (sanitize api_key in logs)
- backend/src/main/kotlin/com/legistrack/service/external/GovInfoApiService.kt (sanitize api_key in logs)
- backend/src/main/kotlin/com/legistrack/controller/DocumentController.kt (clamp page/size; standardized error envelope)
- backend/settings.gradle.kts (use version catalog for Foojay plugin)
- frontend/package.json (pin exact dependency versions)
- issues.md (mark resolved items)

Description:
Implemented stable String keys for Redis cache to match StringRedisSerializer; sanitized sensitive query params from external API debug logs; added pagination input clamping and standardized error envelopes in controllers; removed hardcoded Foojay plugin version by sourcing from the version catalog; pinned frontend dependency versions to exact values per rules.

Impact assessment:
- Improves cache compatibility and avoids potential key serialization issues.
- Enhances security by preventing API key leakage in logs.
- Hardens API correctness and resilience with validated paging and consistent error responses.
- Reduces version drift and duplication in Gradle settings.
- Ensures deterministic frontend builds.

Developer initials: AI, JH

## 2025-08-15 19:25 - [CONFIG] Update issues.md with concrete gaps and align with rules

Files changed:
- issues.md (added items: Redis key generator serialization mismatch; Foojay plugin duplication unresolved; error envelope consistency; request paging validation; frontend exact version pinning; log sanitization for API keys)
- CHANGES.md (this entry)

Description:
Expanded open issues with precise, actionable items discovered during review: ensure Redis keys are stable strings; avoid duplicating Foojay plugin versions; standardize error responses; clamp page/size; pin frontend dependencies to exact versions; and sanitize sensitive query params in logs.

Impact assessment:
- Documentation-only; sets guardrails for upcoming fixes across backend and frontend.
- Improves security posture and correctness alignment without runtime changes.

Developer initials: AI, JH

## 2025-08-15 19:12 - [REFACTOR] Consolidate dependency rules into unified Version & Dependency Management section

Files changed:
- .github/copilot-instructions.md (merged former Dependency & Gradle Version Catalog rules into Version & Dependency Management; removed duplicate section)
- CHANGES.md (this entry)

Description:
Streamlined instructions by merging separate dependency catalog section into a single "Version & Dependency Management" block covering catalog-only declarations, bundling rules, stability hierarchy, forbidden raw coordinates, and enforcement. Eliminates redundancy while preserving all constraints.

Impact assessment:
- Documentation-only consolidation; zero runtime or build logic changes.
- Slight token reduction improving assistant parsing efficiency.

Developer initials: AI, JH

## 2025-08-15 19:05 - [REFACTOR] Enforce Gradle version catalog bundling & architecture abstraction guidance

Files changed:
- .github/copilot-instructions.md (added Dependency & Gradle Version Catalog section + Architecture Abstraction & Flexibility section)
- CHANGES.md (this entry)

Description:
Introduced explicit REQUIRED/FORBIDDEN rules mandating all dependencies/plugins/BOMs be declared exclusively via `gradle/libs.versions.toml` with domain-focused bundles; prohibited raw coordinates or inline versions in build scripts. Added concise architecture abstraction guidance (ports/adapters pattern, interface-first boundaries, config injection) to reinforce flexible, swappable design. Content kept terse for token efficiency; no runtime behavior changes.

Impact assessment:
- Documentation-only; strengthens consistency and future scalability (easier upgrades, reduced drift).
- Low risk: purely textual; build logic already aligned with catalog usage.

Developer initials: AI, JH

## 2025-08-15 18:55 - [REFACTOR] Add open issues alignment section to AI instructions

Files changed:
- .github/copilot-instructions.md (added "Open Issues Alignment" section summarizing active concerns from issues.md to prevent regressions)
- CHANGES.md (this entry)

Description:
Embedded a concise alignment section mapping key unresolved issues (coroutines misuse, caching consistency, DTO nullability, query invalidation, resilience improvements) to actionable guardrails so AI assistants do not reintroduce known problems.

Impact assessment:
- Documentation-only; improves preventive guidance. No runtime impact.
- Low risk: additive text referencing existing issues.md content.

Developer initials: AI, JH

## 2025-08-15 18:48 - [CONFIG] Switch Ollama model to gpt-oss:20b

Files changed:
- backend/src/main/resources/application.properties (updated app.ollama.model)
- backend/src/main/kotlin/com/legistrack/service/DocumentService.kt (updated modelUsed field)
- backend/src/main/kotlin/com/legistrack/service/DataIngestionService.kt (updated modelUsed field)
- .github/copilot-instructions.md (model reference updated)
- CHANGES.md (this entry)

Description:
Standardized on `gpt-oss:20b` model for AI analyses replacing prior `0xroyce/plutus` to align with current local model availability and consolidate configuration naming across docs and services.

Impact assessment:
- Affects future AI analysis records (modelUsed value) only; no schema changes.
- Low risk: purely configuration/string update; existing analyses remain with old modelUsed label for traceability.

Developer initials: AI, JH

## 2025-08-15 18:40 - [REFACTOR] Add ultra-concise agent primer section

Files changed:
- .github/copilot-instructions.md (prepended new 60-second primer section for faster AI onboarding)
- CHANGES.md (this entry)

Description:
Inserted an "Ultra-Concise Agent Primer" (architecture, golden do/don't rules, key commands) above existing TL;DR to reduce ramp time and token consumption when initializing AI context. Preserved all prior detailed enforcement sections unchanged.

Impact assessment:
- Improves assistant productivity; no runtime behavior changes or schema impact.
- Low risk: additive documentation only.

Developer initials: AI, JH

## 2025-08-15 18:29 - [REFACTOR] Introduce project success priority hierarchy & rule tagging

Files changed:
- .github/copilot-instructions.md (added Project Success Priorities section; annotated key rules with [SEC][DATA][CORR][REL][PERF][MAIN][VELO][STYLE] tags; refined enforcement language)
- CHANGES.md (this entry)

Description:
Shifted rules emphasis from user preference to explicit outcome hierarchy (security → style). Added lightweight tags to critical rules for faster conflict resolution and clarified trade-off guidance.

Impact assessment:
- Improves decision clarity during conflicting optimizations; no runtime code changes.
- Low risk: semantic meaning preserved; tags additive.

Developer initials: AI, JH

## 2025-08-15 18:22 - [REFACTOR] Compress AI instructions for lower token usage

Files changed:
- .github/copilot-instructions.md (condensed wording, removed redundancy, preserved semantics)
- CHANGES.md (this entry)

Description:
Reduced verbosity in TL;DR and rules sections (shortened headings, merged repetitive bullets, concise phrasing) to lower token consumption while keeping all enforcement meanings intact.

Impact assessment:
- Faster AI context ingestion; no functional code changes.
- Minimal risk: semantics preserved; revert possible via VCS if nuance later deemed missing.

Developer initials: AI, JH

## 2025-08-15 18:15 - [CONFIG] Refine version management priority to emphasize stability/compatibility

Files changed:
- .github/copilot-instructions.md (updated Version Management Standards section wording)
- CHANGES.md (this entry)

Description:
Revised version selection PATTERN from simple LTS > Latest > RC ordering to an explicit stability & ecosystem compatibility hierarchy favoring supported LTS with broad plugin/tooling compatibility, then widely adopted stable, then latest stable only when low risk, and RC only for critical fixes. Clarifies rejection criteria for premature upgrades.

Impact assessment:
- Improves guidance for dependency upgrades; no functional code changes.
- Reduces risk of introducing breaking changes via overly aggressive version bumps.

Developer initials: AI, JH

## 2025-08-15 18:10 - [REFACTOR] Add concise TL;DR section to AI assistant instructions

Files changed:
- .github/copilot-instructions.md (prepended 40-line quickstart / enforcement summary)
- CHANGES.md (this entry)

Description:
Added a distilled TL;DR section summarizing architecture, workflows, mandatory rules, and frequent gotchas for faster AI agent onboarding while preserving existing detailed enforcement rules.

Impact assessment:
- Improves assistant efficiency; no runtime or behavioral code changes.
- No schema, dependency, or build modifications.

Developer initials: AI, JH

## 2025-08-15 17:55 - [CONFIG] Add MIT license and update README

Files changed:
- LICENSE (added)
- README.md (updated to reference license and authorship)

Description:
Added an MIT license to the project root and updated `README.md` to reference the new license. This change documents project licensing and clarifies terms for reuse and contribution.

Impact assessment:
- Licensing clarifies permitted usage and distribution for contributors and downstream users.
- No code behavior changes; no migrations required.

Developer initials: AI, JH

