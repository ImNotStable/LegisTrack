package com.legistrack.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Configuration for WebClient used in external API calls.
 * Placed in api-rest module so adapters depending on WebClient (e.g., OllamaApiAdapter)
 * receive a bean when the application context starts.
 */
@Configuration
open class WebClientConfig {
    companion object {
        private const val CONNECTION_TIMEOUT_SECONDS = 30L
        private const val RESPONSE_TIMEOUT_SECONDS = 60L
        private const val MAX_IN_MEMORY_SIZE_MB = 10
    }

    @Bean
    open fun webClientBuilder(correlationFilter: ExchangeFilterFunction): WebClient.Builder {
        val httpClient = HttpClient
            .create()
            .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (CONNECTION_TIMEOUT_SECONDS * 1000).toInt())
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_MB * 1024 * 1024) }
            .filter(correlationFilter)
    }

    @Bean
    open fun webClient(builder: WebClient.Builder): WebClient = builder.build()
}
