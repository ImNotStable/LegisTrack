package com.legistrack.health

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class HealthComponentsControllerTest {
    private val service: HealthCheckService = mockk()
    private val controller = HealthController(service)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    private fun sampleAggregate(): AggregateHealth = AggregateHealth(
        success = true,
        status = "DEGRADED",
        components = mapOf(
            "database" to ComponentHealth("UP", latencyMs = 4),
            "cache" to ComponentHealth("DEGRADED", latencyMs = 10, message = "timeout"),
            "ollama" to ComponentHealth("UP"),
            "congressApi" to ComponentHealth("UP")
        )
    )

    @Test
    fun testListComponents(): ResultActions = runBlocking {
        coEvery { service.health() } returns sampleAggregate()
        mockMvc.perform(get("/api/health/components").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.database.status").value("UP"))
            .andExpect(jsonPath("$.cache.status").value("DEGRADED"))
    }

    @Test
    fun testGetSingleComponent(): ResultActions = runBlocking {
        coEvery { service.health() } returns sampleAggregate()
        mockMvc.perform(get("/api/health/database"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun testUnknownComponentReturns404(): ResultActions = runBlocking {
        coEvery { service.health() } returns sampleAggregate()
        mockMvc.perform(get("/api/health/unknownX"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Component 'unknownX' not found"))
    }
}
