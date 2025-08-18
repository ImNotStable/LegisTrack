package com.legistrack.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import com.legistrack.LegisTrackApplication

/**
 * Integration test: start the full Spring Boot application with an intentionally invalid
 * configuration property value to assert that configuration properties validation
 * fails fast during startup ([CORR]/[DATA]).
 */
class InvalidConfigStartupTest {

    @Test
    fun should_failStartup_when_congressBaseUrlInvalid() {
        val app = SpringApplication(LegisTrackApplication::class.java)
        app.setDefaultProperties(
            mapOf(
                // invalid protocol triggers @Pattern on baseUrl
                "app.congress.api.base-url" to "ftp://invalid",
                // provide required fields with otherwise valid values
                "app.congress.api.key" to "k",
                "app.congress.api.retry-attempts" to "0",
                // keep scheduler disabled to minimize side-effects
                "app.scheduler.data-ingestion.enabled" to "false"
            )
        )
        val failed = try {
            val ctx: ConfigurableApplicationContext = app.run()
            // if startup succeeds, close context and mark failure (we expected validation to abort)
            ctx.close()
            false
        } catch (ex: Exception) {
            // Traverse cause chain looking for binding / validation indicators or our custom message
            tailrec fun matches(t: Throwable?): Boolean {
                if (t == null) return false
                val name = t.javaClass.simpleName.lowercase()
                val msg = (t.message ?: "").lowercase()
                if (name.contains("bind") || name.contains("validation") || msg.contains("baseurl must start")) return true
                return matches(t.cause)
            }
            matches(ex)
        }
        assertTrue(failed, "Application should fail to start with invalid congress base URL protocol")
    }
}
