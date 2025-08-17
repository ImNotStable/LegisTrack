package com.legistrack.controller

import com.legistrack.api.GlobalExceptionHandler
import com.legistrack.api.NotFoundException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.slf4j.MDC

/**
 * Focused unit test for exception metrics without booting Spring context.
 */
class GlobalExceptionMetricsUnitTest {

    private val registry = SimpleMeterRegistry()
    private val handler = GlobalExceptionHandler(registry)

    @Test
    fun `records not found and generic exceptions`() {
        MDC.put("correlationId", "unit-cid-1")
        val notFoundResponse = handler.handleNotFound(NotFoundException("absent"))
        val genericResponse = handler.handleGeneric(RuntimeException("boom"))

        val total = registry.counter("api.exceptions.total").count()
        val byTypeNotFound = registry.counter("api.exceptions.byType", "type", "NotFound").count()
        val byTypeGeneric = registry.counter("api.exceptions.byType", "type", "Generic").count()

        assertThat(total).isEqualTo(2.0)
        assertThat(byTypeNotFound).isEqualTo(1.0)
        assertThat(byTypeGeneric).isEqualTo(1.0)

        // Correlation id should be propagated into responses
        assertThat(notFoundResponse.body?.correlationId).isEqualTo("unit-cid-1")
        assertThat(genericResponse.body?.correlationId).isEqualTo("unit-cid-1")
    }

    @AfterEach
    fun cleanupMdc() {
        MDC.clear()
    }
}
