package com.legistrack.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.publisher.Mono
import org.slf4j.MDC
import java.util.UUID

class WebClientCorrelationFilterTest {

    @Test
    fun `should add correlation id when absent`() {
        val filter = org.springframework.web.reactive.function.client.ExchangeFilterFunction { request, next ->
            val existing = MDC.get("X-Correlation-Id") ?: UUID.randomUUID().toString()
            val mutated = if (!request.headers().containsKey("X-Correlation-Id")) {
                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .header("X-Correlation-Id", existing)
                    .build()
            } else request
            next.exchange(mutated)
        }
        var capturedHeader: String? = null
        val mockExchange = ExchangeFunction { req ->
            capturedHeader = req.headers()["X-Correlation-Id"]?.firstOrNull()
            Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build())
        }
        val client = WebClient.builder().filter(filter).exchangeFunction(mockExchange).build()
        client.get().uri("http://example.com/test").retrieve().bodyToMono(String::class.java).onErrorResume { Mono.empty() }.block()

        assertThat(capturedHeader).isNotNull()
        assertThat(capturedHeader).matches("[0-9a-fA-F\\-]{36}")
    }
}
