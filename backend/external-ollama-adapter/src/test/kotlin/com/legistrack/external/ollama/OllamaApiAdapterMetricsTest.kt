package com.legistrack.external.ollama

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class OllamaApiAdapterMetricsTest {

    private fun webClientWith(exchange: ExchangeFunction): WebClient = WebClient.builder().exchangeFunction(exchange).build()

    private fun exchange(status: HttpStatus, body: String = "{\"response\":\"ok\"}" ) = ExchangeFunction { _: ClientRequest ->
        val h = HttpHeaders()
        h.add("Content-Type", "application/json")
        Mono.just(
            ClientResponse.create(status)
                .headers { it.addAll(h) }
                .body(body)
                .build()
        )
    }

    @Test
    fun metricsIncrementOnSuccess() = runBlocking {
        val registry = SimpleMeterRegistry()
        val props = OllamaProperties(baseUrl = "https://ollama.example", model = "mock", bootstrapEnabled = false)
        val adapter = OllamaApiAdapter(
            webClient = webClientWith(exchange(HttpStatus.OK)),
            props = props,
            meterRegistry = registry
        )
        // Mark service ready to bypass readiness guard
        val serviceReadyField = OllamaApiAdapter::class.java.getDeclaredField("serviceReady").apply { isAccessible = true }
        serviceReadyField.set(adapter, true)
        adapter.generateAnalysis("test", 0.1)
        assertEquals(1, registry.counter("ollama.api.requests").count().toInt())
        // No errors expected
        assertEquals(0, registry.counter("ollama.api.errors").count().toInt())
        // Latency timer should have at least one record
        val timer = registry.get("ollama.api.latency").timer()
        assert(timer.count() == 1L) { "Expected latency timer count=1 but was ${timer.count()}" }
    }

    @Test
    fun metricsIncrementOnFailure() = runBlocking {
        val registry = SimpleMeterRegistry()
        val props = OllamaProperties(baseUrl = "https://ollama.example", model = "mock", bootstrapEnabled = false)
        val adapter = OllamaApiAdapter(
            webClient = webClientWith(exchange(HttpStatus.INTERNAL_SERVER_ERROR)),
            props = props,
            meterRegistry = registry
        )
        val serviceReadyField = OllamaApiAdapter::class.java.getDeclaredField("serviceReady").apply { isAccessible = true }
        serviceReadyField.set(adapter, true)
        adapter.generateAnalysis("test", 0.1)
        assertEquals(1, registry.counter("ollama.api.requests").count().toInt())
        assertEquals(1, registry.counter("ollama.api.errors").count().toInt())
    }
}
