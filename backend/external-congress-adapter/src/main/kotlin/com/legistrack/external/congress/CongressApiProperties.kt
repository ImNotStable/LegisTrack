package com.legistrack.external.congress

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@ConfigurationProperties(prefix = "app.congress.api")
@Validated
data class CongressApiProperties(
    @field:NotBlank
    var key: String = "dummy-test-key",
    @field:NotBlank
    @field:Pattern(regexp = "https?://.+", message = "baseUrl must start with http/https")
    var baseUrl: String = "https://api.congress.gov/v3",
    @field:Min(0)
    var retryAttempts: Long = 3,
    var cb: Cb = Cb(),
    @field:Min(0)
    @field:Max(100)
    var retryAdaptiveThresholdPercent: Double = 10.0
) {
    data class Cb(
        @field:Min(1)
        var threshold: Int = 5,
        @field:Min(1)
        var cooldownSeconds: Long = 30
    )
    val breakerThreshold: Int get() = cb.threshold
    val breakerCooldownSeconds: Long get() = cb.cooldownSeconds
}
