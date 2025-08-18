package com.legistrack.external.congress

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CongressApiPropertiesValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun should_fail_when_retryAttempts_negative() {
        val props = CongressApiProperties(
            key = "k",
            baseUrl = "https://example.com",
            retryAttempts = -1,
        )
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "retryAttempts" }, "Expected violation on retryAttempts, got $violations")
    }

    @Test
    fun should_fail_when_baseUrl_invalid() {
        val props = CongressApiProperties(
            key = "k",
            baseUrl = "ftp://bad",
            retryAttempts = 0,
        )
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "baseUrl" }, "Expected violation on baseUrl, got $violations")
    }
}
