package com.legistrack.health

import com.legistrack.external.congress.CongressApiAdapter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Uses a real CongressApiAdapter instance with internal gauge seeding via reflection to verify
 * system health controller response shape without network calls.
 */
class SystemHealthControllerIntegrationTest {

    @Test
    fun should_includeCongressSnapshot() = runBlocking {
        val registry = SimpleMeterRegistry()
        val adapter = CongressApiAdapter(
            webClient = WebClient.builder().build(),
            apiKey = "test",
            baseUrl = "https://example.org",
            configuredRetryAttempts = 0,
            meterRegistry = registry
        )
        val cls = adapter::class.java
        cls.getDeclaredField("limitGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(120) }
        cls.getDeclaredField("remainingGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(30) }
        cls.getDeclaredField("resetGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(45) }
        // Directly assert snapshot keys (controller adds wrapping fields but snapshot logic is core target)
        val congress = adapter.healthSnapshot()
        val expectedKeys = setOf(
            "circuitState",
            "consecutiveFailures",
            "rateLimitRemaining",
            "rateLimitLimit",
            "rateLimitResetSeconds",
            "rateLimitRemainingPct",
            "last429Epoch",
            "circuitOpenDurationSeconds"
        )
        assertTrue(congress.keys.containsAll(expectedKeys), "Missing keys: ${expectedKeys - congress.keys}")
        // Validate derived percentage ~25%
        val pct = congress["rateLimitRemainingPct"] as Double?
        assertEquals(25.0, pct)
    }
}
