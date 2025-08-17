package com.legistrack.controller

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
class DocumentNotFoundCorrelationIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun should_propagateCorrelationId_on_404() {
        val cid = "it-corr-404"
        webTestClient.get()
            .uri("/api/documents/999999")
            .header("X-Correlation-Id", cid)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.correlationId").isEqualTo(cid)
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").isEqualTo("Document not found")
    }
}
