package com.legistrack.controller

import com.legistrack.api.GlobalExceptionHandler
import com.legistrack.api.NotFoundException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.ResponseEntity

class GlobalExceptionHandlerTest {
    private val registry = SimpleMeterRegistry()
    private val handler = GlobalExceptionHandler(registry)

    @Test
    fun should_includeCorrelationId_andIncrementCounters_on_Generic() {
        MDC.put("correlationId", "test-cid")
        val resp = handler.handleGeneric(RuntimeException("boom"))
        assert(resp.statusCode.is5xxServerError)
        val body = resp.body!!
        assert(body.correlationId == "test-cid")
        assert(registry.counter("api.exceptions.total").count() == 1.0)
        assert(registry.counter("api.exceptions.byType", "type", "Generic").count() == 1.0)
    }

    @Test
    fun should_mapNotFoundException_to404() {
        MDC.put("correlationId", "cid-404")
        val resp: ResponseEntity<com.legistrack.api.ErrorResponse> = handler.handleNotFound(NotFoundException("Missing"))
        assert(resp.statusCode.value() == 404)
        assert(resp.body!!.message == "Missing")
        assert(resp.body!!.correlationId == "cid-404")
    }
}
