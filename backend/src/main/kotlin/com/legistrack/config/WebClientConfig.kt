package com.legistrack.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Configuration for WebClient used in external API calls.
 *
 * Provides optimized settings for HTTP client connections,
 * timeouts, and memory usage.
 */
@Configuration
class WebClientConfig {
    companion object {
        private const val CONNECTION_TIMEOUT_SECONDS = 30L
        private const val RESPONSE_TIMEOUT_SECONDS = 60L
        private const val MAX_IN_MEMORY_SIZE_MB = 10
    }

    /**
     * Creates a configured WebClient bean for HTTP operations.
     *
     * @return WebClient instance with optimized settings
     */
    @Bean
    fun webClient(): WebClient {
        val httpClient =
            HttpClient
                .create()
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                .option(
                    io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    (CONNECTION_TIMEOUT_SECONDS * 1000).toInt(),
                )

        return WebClient
            .builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_MB * 1024 * 1024)
            }.build()
    }
}
