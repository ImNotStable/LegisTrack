package com.legistrack.controller

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.beans.factory.annotation.Autowired
import com.legistrack.testsupport.PostgresTestContainerConfig

/**
 * Regression test: ensure page serialization does not leak Spring Data PageImpl type details
 * and uses DTO shape (via @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [PostgresTestContainerConfig::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class DocumentsPageSerializationIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should_serializeDocumentsPage_withoutPageImplType`() {
        webTestClient.get().uri("/api/documents?page=0&size=1&sort=introductionDate,desc")
            .exchange()
            .expectStatus().isOk
            // Should contain core pagination fields
            .expectBody()
            .jsonPath("$.pageable").doesNotExist() // VIA_DTO removes Spring internal pageable block
            .jsonPath("$.content").exists()
            .jsonPath("$.pageNumber").exists()
            .jsonPath("$.pageSize").exists()
            .jsonPath("$.totalElements").exists()
            .jsonPath("$.totalPages").exists()
            // Ensure no PageImpl classname leakage
            .consumeWith { resp ->
                val body = resp.responseBody?.toString(Charsets.UTF_8) ?: ""
                assert(!body.contains("PageImpl")) { "Response should not contain PageImpl implementation detail" }
            }
    }
}
