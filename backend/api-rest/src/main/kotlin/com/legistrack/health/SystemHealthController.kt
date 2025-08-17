package com.legistrack.health

import com.legistrack.external.congress.CongressApiAdapter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Aggregated system health that includes the existing component aggregation plus
 * a cheap snapshot of Congress API circuit + rate limit state. Avoids triggering
 * network calls and reuses existing health aggregation for overall status.
 */
 data class SystemHealthResponse(
     val success: Boolean,
     val status: String,
     val congress: Map<String, Any?>,
     val timestamp: Long = System.currentTimeMillis()
 )

@RestController
@RequestMapping("/api/system/health")
class SystemHealthController(
    private val congressApiAdapter: CongressApiAdapter,
    private val healthCheckService: HealthCheckService
) {
    @GetMapping
    suspend fun systemHealth(): ResponseEntity<Any> {
        val base = healthCheckService.health()
        val congressSnapshot = congressApiAdapter.healthSnapshot()
        val response = SystemHealthResponse(
            success = base.success,
            status = base.status,
            congress = congressSnapshot
        )
        return ResponseEntity.ok(response as Any)
    }
}
