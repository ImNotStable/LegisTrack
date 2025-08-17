package com.legistrack.api

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler(private val meterRegistry: MeterRegistry) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val totalCounter = meterRegistry.counter("api.exceptions.total")

    private fun record(type: String) {
        totalCounter.increment()
        meterRegistry.counter("api.exceptions.byType", "type", type).increment()
    }

    private fun err(message: String, type: String, details: Map<String, Any?>? = null): ErrorResponse {
        val cid = MDC.get("correlationId")
        record(type)
        return ErrorResponse(message = message, details = details, correlationId = cid)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(err("Missing required parameter '${ex.parameterName}'", "MissingParam"))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(err("Invalid value for parameter '${ex.name}'", "TypeMismatch"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val msg = ex.bindingResult.fieldErrors.joinToString { "${it.field} ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(err(msg.ifBlank { "Validation failed" }, "Validation"))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(ex.message ?: "Not found", "NotFound"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(err("Internal server error", "Generic"))
    }
}
