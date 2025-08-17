package com.legistrack.health

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest(
    classes = [HealthControllerCorrelationIdTest.TestConfig::class],
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:cidtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerCorrelationIdTest {
    @Configuration
    class TestConfig {
        @Bean fun healthCheckService(): HealthCheckService = mockk(relaxed = true)
        @Bean fun correlationIdFilter() = com.legistrack.config.CorrelationIdFilter()
        @Bean fun healthController(healthCheckService: HealthCheckService) = HealthController(healthCheckService)
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var healthCheckService: HealthCheckService

    @Test
    fun testCorrelationIdHeaderEchoesInResponse() = runBlocking {
        val cid = "test-corr-123"
        coEvery { healthCheckService.health() } answers {
            AggregateHealth(
                success = true,
                status = "UP",
                components = emptyMap(),
                correlationId = MDC.get("correlationId")
            )
        }
        mockMvc.perform(get("/api/health").header("X-Correlation-Id", cid))
            .andExpect(header().string("X-Correlation-Id", cid))
            .andExpect(jsonPath("$.correlationId").value(cid))
    }

    // sanity test removed; explicit verification added instead
    @Test
    fun verifyServiceInvocation() = runBlocking {
        coEvery { healthCheckService.health() } returns AggregateHealth(true, "UP", emptyMap(), correlationId = "abc")
        mockMvc.perform(get("/api/health"))
        coVerify(exactly = 1) { healthCheckService.health() }
    }
}
