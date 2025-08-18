# LegisTrack Knowledge Base

Welcome to the internal Obsidian vault. Start here for architecture, operations, and decision context.

## Quick Start
* Read [[Architecture Overview]] for system flow & module boundaries.
* Use [[Runbook - Ingestion Pipeline]] to operate or debug ingestion.
* Use [[Runbook - AI Analysis]] for AI generation workflow.
* Reference [[Glossary]] for canonical term definitions.
* Check [[Troubleshooting]] for common issues.
* Review active decisions (e.g., [[ADR-0001-multi-module-architecture]]) or propose new ones using [[ADR-TEMPLATE]].

## Editing Guidelines
* Prefer updating existing docs; create new ADRs for decisions, not ad-hoc notes.
* Use priority ladder tags (SEC, DATA, CORR, REL, PERF, MAIN, VELO, STYLE) in ADRs to explain trade-offs.
* Never store secrets or transient logs.

## Document Map
| Category | Document |
|----------|----------|
| Architecture | [[Architecture Overview]] |
| Operations | [[Runbook - Ingestion Pipeline]] / [[Runbook - AI Analysis]] |
| Resilience | [[Troubleshooting]] |
| Domain | [[Domain Model Mapping]] / [[Glossary]] |
| Decisions | [[ADR-0001-multi-module-architecture]] / [[ADR-TEMPLATE]] |

## Next Steps
If you identify a gap:
1. Confirm it isn't already covered.
2. Draft change or ADR referencing priority ladder.
3. Submit PR including vault update.

Happy tracking!