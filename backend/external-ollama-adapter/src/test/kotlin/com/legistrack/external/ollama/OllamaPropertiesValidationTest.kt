package com.legistrack.external.ollama

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OllamaPropertiesValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun should_fail_when_model_blank() {
        val props = OllamaProperties(baseUrl = "http://localhost:11434", model = "")
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "model" }, "Expected violation on model, got $violations")
    }

    @Test
    fun should_fail_when_baseUrl_invalidProtocol() {
        val props = OllamaProperties(baseUrl = "ws://localhost:9999", model = "foo")
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "baseUrl" }, "Expected violation on baseUrl, got $violations")
    }
}
