package com.legistrack.health

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HealthDetailEndpointsTest {
    private val service: HealthCheckService = mockk()

    @Test
    fun should_return_congress_component() = runBlocking {
        val components = mapOf(
            "congressApi" to ComponentHealth(status = "UP", latencyMs = 10, lastSuccessEpochMs = 1000L),
            "ollama" to ComponentHealth(status = "UP", latencyMs = 5, lastSuccessEpochMs = 1000L),
            "database" to ComponentHealth(status = "UP", latencyMs = 3, lastSuccessEpochMs = 1000L),
            "cache" to ComponentHealth(status = "DEGRADED", latencyMs = 8, lastSuccessEpochMs = 900L)
        )
        coEvery { service.health() } returns AggregateHealth(true, "UP", components)
        val controller = HealthController(service)
        val response = controller.congress()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body as ComponentHealth
        assertThat(body.status).isEqualTo("UP")
    }

    @Test
    fun should_return_ollama_component() = runBlocking {
        val components = mapOf(
            "congressApi" to ComponentHealth(status = "UP", latencyMs = 10, lastSuccessEpochMs = 1000L),
            "ollama" to ComponentHealth(status = "DEGRADED", latencyMs = 15, lastSuccessEpochMs = 900L),
            "database" to ComponentHealth(status = "UP", latencyMs = 4, lastSuccessEpochMs = 1000L),
            "cache" to ComponentHealth(status = "UP", latencyMs = 6, lastSuccessEpochMs = 950L)
        )
        coEvery { service.health() } returns AggregateHealth(true, "DEGRADED", components)
        val controller = HealthController(service)
        val response = controller.ollama()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body as ComponentHealth
        assertThat(body.status).isEqualTo("DEGRADED")
    }

    @Test
    fun should_return_database_component() = runBlocking {
        val components = mapOf(
            "database" to ComponentHealth(status = "UP", latencyMs = 12, lastSuccessEpochMs = 1000L)
        )
        coEvery { service.health() } returns AggregateHealth(true, "UP", components)
        val controller = HealthController(service)
        val response = controller.database()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body as ComponentHealth
        assertThat(body.status).isEqualTo("UP")
    }

    @Test
    fun should_return_cache_component() = runBlocking {
        val components = mapOf(
            "cache" to ComponentHealth(status = "DEGRADED", latencyMs = 20, lastSuccessEpochMs = 500L)
        )
        coEvery { service.health() } returns AggregateHealth(true, "DEGRADED", components)
        val controller = HealthController(service)
        val response = controller.cache()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body as ComponentHealth
        assertThat(body.status).isEqualTo("DEGRADED")
    }
}
