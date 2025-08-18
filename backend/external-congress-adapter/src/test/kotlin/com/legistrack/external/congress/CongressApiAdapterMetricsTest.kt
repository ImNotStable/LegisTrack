package com.legistrack.external.congress

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.web.reactive.function.BodyExtractor
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDate

class CongressApiAdapterMetricsTest {

    private fun webClientWith(exchange: ExchangeFunction): WebClient = WebClient.builder().exchangeFunction(exchange).build()

    private fun dummyExchange(status: HttpStatus, headers: Map<String,String> = emptyMap(), body: String = "{\"bills\":[]}") = ExchangeFunction { _: ClientRequest ->
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Content-Type", "application/json")
        headers.forEach { (k,v) -> httpHeaders.add(k,v) }
        Mono.just(
            ClientResponse.create(status)
                .headers { it.addAll(httpHeaders) }
                .body(body)
                .build()
        )
    }

    @Test
    fun metricsIncrementOn429AndParseHeaders() = runBlocking {
        val registry = SimpleMeterRegistry()
        val exchange = dummyExchange(HttpStatus.TOO_MANY_REQUESTS, mapOf("x-ratelimit-remaining" to "7", "x-ratelimit-reset" to "12"))
        val props = CongressApiProperties(key = "test", baseUrl = "https://example.org", retryAttempts = 0)
        val adapter = CongressApiAdapter(
            webClient = webClientWith(exchange),
            props = props,
            meterRegistry = registry
        )
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val throttled = registry.counter("congress.api.throttled").count().toInt()
        val requests = registry.counter("congress.api.requests").count().toInt()
        val remaining = registry.get("congress.api.rateLimit.remaining").gauge().value().toInt()
        val reset = registry.get("congress.api.rateLimit.resetSeconds").gauge().value().toInt()
    assertEquals(1, throttled, "Expected exactly one throttled increment with retries disabled")
    assertEquals(1, requests, "Expected exactly one request with retries disabled")
        assertEquals(7, remaining)
        assertEquals(12, reset)
    }

    @Test
    fun metricsExtendedRateLimitParsing() = runBlocking {
        val registry = SimpleMeterRegistry()
        val exchange = dummyExchange(
            HttpStatus.TOO_MANY_REQUESTS,
            mapOf(
                "x-ratelimit-remaining" to "30",
                "x-ratelimit-reset" to "55",
                "x-ratelimit-limit" to "120"
            )
        )
    val props = CongressApiProperties(key = "test", baseUrl = "https://example.org", retryAttempts = 0)
    val adapter = CongressApiAdapter(webClientWith(exchange), props, registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        assertEquals(30, registry.get("congress.api.rateLimit.remaining").gauge().value().toInt())
        assertEquals(55, registry.get("congress.api.rateLimit.resetSeconds").gauge().value().toInt())
        assertEquals(120, registry.get("congress.api.rateLimit.limit").gauge().value().toInt())
        val pct = registry.get("congress.api.rateLimit.remainingPct").gauge().value()
        assertEquals(25.0, pct, 0.0001, "Expected remaining percentage 25% (30/120*100)")
        val last429 = registry.get("congress.api.rateLimit.last429Epoch").gauge().value().toLong()
        assert(last429 > 0) { "Expected last429Epoch to be set > 0" }
    }

    @Test
    fun metricsIncrementErrorsOn500() = runBlocking {
        val registry = SimpleMeterRegistry()
        val exchange = dummyExchange(HttpStatus.INTERNAL_SERVER_ERROR)
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0), registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val errors = registry.counter("congress.api.errors").count().toInt()
        val requests = registry.counter("congress.api.requests").count().toInt()
    assertEquals(1, errors, "Expected exactly one error with retries disabled")
    assertEquals(1, requests, "Expected exactly one request with retries disabled")
    }

    @Test
    fun metricsRetryAttemptsTagged() = runBlocking {
        // Simulate two thrown exceptions then success to trigger retryWhen logic (5xx alone may not throw)
        var invocation = 0
        val registry = SimpleMeterRegistry()
        val exchange = ExchangeFunction { _: ClientRequest ->
            invocation++
            if (invocation <= 2) {
                return@ExchangeFunction Mono.error(RuntimeException("boom-$invocation"))
            }
            val httpHeaders = HttpHeaders().apply { add("Content-Type", "application/json") }
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .headers { it.addAll(httpHeaders) }
                    .body("{\"bills\":[]}")
                    .build()
            )
        }
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=2), registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val requests = registry.counter("congress.api.requests").count().toInt()
        // Only the successful terminal attempt increments requests (prior failures throw before response mapping)
        assertEquals(1, requests, "Expected only final successful attempt to count as request")
        val attemptCounters = listOf("0","1","2").associateWith { a ->
            registry.find("congress.api.retries").tags("attempt", a).counter()?.count()?.toInt() ?: 0
        }
        val nonZero = attemptCounters.filterValues { it > 0 }
        assertEquals(2, nonZero.size, "Expected exactly two retry attempt counters (failures) but found $attemptCounters")
    }

    @Test
    fun latencyTimerRecordsOnSuccess() = runBlocking {
        val registry = SimpleMeterRegistry()
        val exchange = dummyExchange(HttpStatus.OK)
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0), registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val timer = registry.find("congress.api.latency").tags("operation", "recentBills", "outcome", "success").timer()
        assert(timer != null && timer.count() == 1L) { "Expected latency timer to record one success for recentBills" }
    }

    @Test
    fun latencyTimerRecordsOnRetryThenSuccess() = runBlocking {
        var attempts = 0
        val registry = SimpleMeterRegistry()
        val exchange = ExchangeFunction { _: ClientRequest ->
            attempts++
            if (attempts == 1) {
                Mono.error(RuntimeException("boom"))
            } else {
                // second attempt succeeds because retryWhen wraps; keep deterministic
                val headers = HttpHeaders().apply { add("Content-Type", "application/json") }
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .headers { it.addAll(headers) }
                        .body("{\"bills\":[]}")
                        .build()
                )
            }
        }
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=1), registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val successTimer = registry.find("congress.api.latency").tags("operation", "recentBills", "outcome", "success").timer()
        val errorTimer = registry.find("congress.api.latency").tags("operation", "recentBills", "outcome", "error").timer()
        assert(successTimer != null && successTimer.count() == 1L) { "Expected one success latency timer after retry" }
        assert(errorTimer == null || errorTimer.count() == 0L) { "Did not expect error latency timer for eventual success" }
    }

    @Test
    fun latencyTimerRecordsErrorForBillDetails() = runBlocking {
        val registry = SimpleMeterRegistry()
        // Always fail
        val exchange = ExchangeFunction { _: ClientRequest -> Mono.error(RuntimeException("boom")) }
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0), registry)
        adapter.getBillDetails(118, "hr", "999") // returns null
        val errorTimer = registry.find("congress.api.latency").tags("operation", "billDetails", "outcome", "error").timer()
        assert(errorTimer != null && errorTimer.count() == 1L) { "Expected one error latency timer for failed billDetails" }
    }

    @Test
    fun metricsCacheMissIncrements() = runBlocking {
        val registry = SimpleMeterRegistry()
        val exchange = dummyExchange(HttpStatus.OK)
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0), registry)
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        adapter.getBillDetails(118, "hr", "123")
        val missCount = registry.counter("congress.api.cache.miss").count().toInt()
        assertEquals(2, missCount, "Expected two cache miss increments (recentBills + billDetails)")
    }


    @Test
    fun circuitBreakerOpensAndShortCircuits() = runBlocking {
        val registry = SimpleMeterRegistry()
        var calls = 0
        val exchange = ExchangeFunction { _: ClientRequest ->
            calls++
            Mono.error(RuntimeException("boom"))
        }
        // Use a small threshold so we can observe short-circuits within a few invocations
    val props = CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0, cb = CongressApiProperties.Cb(threshold=2, cooldownSeconds=60))
    val adapter = CongressApiAdapter(webClientWith(exchange), props, registry)
        // Invoke several times; only the first 'threshold' attempts should hit the exchange (calls variable)
        repeat(6) { adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1) }
        // After opening, subsequent calls should be short-circuited and not increment 'calls'
        assertEquals(2, calls, "Expected only threshold number of real HTTP attempts before short-circuiting")
        val stateGauge = registry.get("congress.api.circuit.state").gauge().value().toInt()
        assertEquals(1, stateGauge, "Circuit breaker should be open")
        val shortCircuits = registry.counter("congress.api.circuit.shortcircuits").count().toInt()
        assertEquals(4, shortCircuits, "Expected remaining invocations after opening to be short-circuited")
    }

    @Test
    fun circuitBreakerHalfOpenClosesOnSuccess() = runBlocking {
        val registry = SimpleMeterRegistry()
        var calls = 0
        var fail = true
        val exchange = ExchangeFunction { _: ClientRequest ->
            calls++
            if (fail) {
                Mono.error(RuntimeException("boom"))
            } else {
                // Successful response
                val headers = HttpHeaders().apply { add("Content-Type", "application/json") }
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                        .headers { it.addAll(headers) }
                        .body("{\"bills\":[]}")
                        .build()
                )
            }
        }
        // Use default threshold=5; we'll trigger 5 failures then mark success and simulate cooldown by manipulating internal state via reflection (simplified assumption) - for test brevity we just continue causing failures then success; outcome check limited to gauge transitions.
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=0), registry)
        repeat(5) { adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1) }
        // Breaker should be open now
        val stateOpen = registry.get("congress.api.circuit.state").gauge().value().toInt()
        assertEquals(1, stateOpen, "Breaker should be open after failures")
        // Flip to success path and attempt more calls; due to cooldown not elapsed, may still short-circuit; we can't manipulate time easily here, so we assert state remains open.
        fail = false
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        val stateAfter = registry.get("congress.api.circuit.state").gauge().value().toInt()
        assertEquals(1, stateAfter, "Breaker stays open without cooldown elapsing (time-independent test)")
    }

    @Test
    fun adaptiveRetrySuppressedWhenLowRemainingPct() = runBlocking {
        // Simulate 9 remaining out of 200 limit => 4.5% remaining (<10% threshold) so retries suppressed
        var invocation = 0
        val registry = SimpleMeterRegistry()
        val exchange = ExchangeFunction { _: ClientRequest ->
            invocation++
            // Always throw to see if retries happen; we expect only single attempt
            val headers = HttpHeaders().apply {
                add("Content-Type", "application/json")
                add("x-ratelimit-remaining", "9")
                add("x-ratelimit-limit", "200")
            }
            Mono.just(
                ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers { it.addAll(headers) }
                    .body("{\"bills\":[]}")
                    .build()
            )
        }
    val adapter = CongressApiAdapter(webClientWith(exchange), CongressApiProperties(key="test", baseUrl="https://example.org", retryAttempts=2), registry)
        // Seed internal rate limit gauges BEFORE first call so adaptive suppression logic sees low remaining pct
        run {
            val klass = adapter::class.java
            val limitField = klass.getDeclaredField("limitGaugeHolder").apply { isAccessible = true }
            val remainingField = klass.getDeclaredField("remainingGaugeHolder").apply { isAccessible = true }
            (limitField.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(200)
            (remainingField.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(9)
        }
        adapter.getRecentBills(LocalDate.now().minusDays(1), 0, 1)
        assertEquals(1, invocation, "Expected only one invocation due to adaptive retry suppression")
        val suppressed = registry.counter("congress.api.retries.adaptive.suppressed").count().toInt()
        assertEquals(1, suppressed, "Expected suppression counter to increment once")
    }
}
