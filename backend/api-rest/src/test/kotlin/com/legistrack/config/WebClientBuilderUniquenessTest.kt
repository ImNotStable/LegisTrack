package com.legistrack.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono

/**
 * Integration-level assurance that after refactor there is exactly one WebClient.Builder bean
 * and that the correlation filter from OutboundCorrelationConfig is applied (adds X-Correlation-Id).
 */
@SpringBootTest(properties = [
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:webclient;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=none"
])
@ActiveProfiles("test")
class WebClientBuilderUniquenessTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var builder: WebClient.Builder

    @Autowired
    private lateinit var correlationFilter: ExchangeFilterFunction

    @Test
    fun `should have single builder bean and inject correlation header`() {
        val builderBeans = applicationContext.getBeansOfType(WebClient.Builder::class.java)
        assertThat(builderBeans).hasSize(1)

        // Build a client overriding exchange function to capture headers
        var seenHeader: String? = null
        val client = builder
            .exchangeFunction { request ->
                seenHeader = request.headers()["X-Correlation-Id"]?.firstOrNull()
                Mono.empty()
            }
            .build()

        client.get().uri("http://dummy.local/test").retrieve().bodyToMono(String::class.java).onErrorResume { Mono.empty() }.block()

        assertThat(seenHeader).describedAs("Correlation header should be injected by filter").isNotNull()
    }
}
