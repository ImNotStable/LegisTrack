package com.legistrack.controller

import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import com.legistrack.testsupport.PostgresTestContainerConfig

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [PostgresTestContainerConfig::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ExceptionMetricsIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Test
    fun should_incrementExceptionCounters_onRepeated404() {
        repeat(2) {
            webTestClient.get().uri("/api/documents/888888").exchange().expectStatus().isNotFound
        }
        val total = meterRegistry.counter("api.exceptions.total").count()
        assert(total >= 2.0) { "Expected at least 2 total exceptions, got $total" }
    }
}
