package com.legistrack.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import java.util.UUID

/**
 * Provides an ExchangeFilterFunction that injects a correlation ID header into
 * outbound WebClient calls. The actual WebClient.Builder bean is defined in
 * [WebClientConfig]; we only expose the filter here to avoid bean duplication
 * / potential cycles where two configs both try to wrap a builder. Works
 * alongside the servlet CorrelationIdFilter for inbound requests.
 */
@Configuration
open class OutboundCorrelationConfig {
    companion object { private const val CORRELATION_HEADER = "X-Correlation-Id" }

    @Bean
    open fun correlationFilter(): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
        val existing = MDC.get(CORRELATION_HEADER) ?: UUID.randomUUID().toString()
        val enriched: ClientRequest = if (!request.headers().containsKey(CORRELATION_HEADER)) {
            ClientRequest.from(request)
                .header(CORRELATION_HEADER, existing)
                .build()
        } else request
        next.exchange(enriched)
    }

}
