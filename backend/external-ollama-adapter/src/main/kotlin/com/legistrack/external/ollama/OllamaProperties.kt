package com.legistrack.external.ollama

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@ConfigurationProperties(prefix = "app.ollama")
@Validated
data class OllamaProperties(
    // baseUrl may be blank in test contexts to allow mock adapter usage
    @field:Pattern(regexp = "|https?://.+", message = "baseUrl must be blank (tests) or start with http/https")
    var baseUrl: String = "",
    @field:NotBlank
    var model: String = "mock",
    var bootstrapEnabled: Boolean = false
)
