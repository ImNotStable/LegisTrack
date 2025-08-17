package com.legistrack.health

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

class HealthControllerTest {
    private val service: HealthCheckService = mockk()

    @Test
    fun `aggregate returns overall status`() = runBlocking {
        coEvery { service.health() } returns AggregateHealth(
            success = true,
            status = "UP",
            components = mapOf(
                "database" to ComponentHealth("UP", latencyMs = 5),
                "cache" to ComponentHealth("UNKNOWN")
            )
        )
        val controller = HealthController(service)
        val response = controller.aggregate()
        val body = response.body!!
        assertEquals("UP", body.status)
        assertEquals(2, body.components.size)
        assertEquals("UP", body.components["database"]?.status)
    }
}
