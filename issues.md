## Task List

- Backend architecture
  - [ ] Mixing coroutines with blocking calls: `suspend` functions use `WebClient.block()`; controllers use `runBlocking` instead of `suspend` endpoints.
  - [ ] Reactive client in a non-reactive MVC stack without proper coroutine bridging (`awaitBody`, `awaitSingle`).

- External API integrations
  - [ ] Congress members endpoint ignores `chamber` parameter; path/query not applied.
  - [ ] Manual map parsing for Congress responses risks type mismatches (e.g., `congress` as String vs Int); use Jackson mapping.
  - [ ] GovInfo granule details path likely incorrect (`/packages/{granuleId}/summary`); include package context.
  - [ ] `isModelAvailable` in Ollama uses substring match; can yield false positives for model names.

- Caching
  - [ ] Many `@Cacheable` caches not explicitly configured in `CacheConfig`; inconsistent TTLs across caches.
  - [ ] Cache keys concatenate primitives into strings; prefer `keyGenerator` or structured keys.

- Configuration
  - [ ] CORS configured twice (via `WebMvcConfigurer` and `CorsFilter`).
  - [ ] Very verbose logging in prod (`logging.level.*=DEBUG`).
  - [ ] Duplicate `check` task dependency on `detekt` in Gradle.
  - [ ] Foojay resolver version duplicated between `settings.gradle.kts` and version catalog.

- Data/DTO correctness
  - [ ] `DocumentDetailDto.actions.actionDate` is non-nullable; upstream data may lack dates.
  - [ ] `getGovInfoText` returns a placeholder (title) instead of actual text.

- Frontend data consistency
  - [ ] Invalidating queries after mutations misses the infinite list key; only `['documents']` is invalidated, not `['documents-infinite', ...]`.
  - [ ] Third bar in party breakdown computes as `100 - dem - rep`; may go negative due to rounding or unexpected percentages.

- Frontend tooling
  - [ ] CRA + React Query v5 + TS 4.9 type friction; align TS to >= 5.
  - [ ] Global `Content-Type: application/json` on all requests (including GET).

- Resilience/rate limits
  - [ ] No circuit-breakers/backoff jitter; retries use fixed backoff.
  - [ ] Unbounded `limit`/`pageSize` from requests; add validation/clamping.

- Startup behavior
  - [ ] Ollama bootstrap waits up to 10/30 minutes in background; may mask readiness and consume resources if misconfigured.

- Security/operational
  - [ ] Credentials via env without secret management abstraction; ensure not logged.
  - [ ] CORS allows credentials with multiple origins; verify exact origins in deployments.

- Gradle/version catalog
  - [ ] Spring Cloud BOM declared in catalog but unused; remove or import via dependency management for consistency.

- UX
  - [ ] `DocumentDetail` label “View Full Text on Congress.gov” bound to `fullTextUrl` that may not be a Congress.gov link.

- Tests
  - [ ] No tests around legislative external services; integration parsing and error handling unverified.

### Quick wins
- [ ] Fix `block()`/`runBlocking` usage in `suspend` paths.
- [ ] Correct Congress/GovInfo endpoints.
- [ ] Unify cache TTLs and configure caches in `CacheConfig`.
- [ ] Deduplicate CORS configuration.
- [ ] Invalidate `['documents-infinite', ...]` after mutations.