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

