package com.legistrack.health

import org.springframework.http.ResponseEntity
import com.legistrack.api.ErrorResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/health")
class HealthController(private val healthCheckService: HealthCheckService) {
    @GetMapping
    suspend fun aggregate(): ResponseEntity<AggregateHealth> = ResponseEntity.ok(healthCheckService.health())

    @GetMapping("/components")
    suspend fun components(): ResponseEntity<Map<String, ComponentHealth>> {
        val agg = healthCheckService.health()
        return ResponseEntity.ok(agg.components)
    }

    @GetMapping("/{name}")
    suspend fun component(@PathVariable name: String): ResponseEntity<Any> {
        val agg = healthCheckService.health()
        val comp = agg.components[name]
        return if (comp != null) ResponseEntity.ok(comp) else ResponseEntity.status(404)
            .body(ErrorResponse(message = "Component '$name' not found"))
    }

    // Convenience explicit endpoints (contract clarity & future expansion with component-specific metrics)
    @GetMapping("/congress")
    suspend fun congress(): ResponseEntity<Any> = component("congressApi")

    @GetMapping("/ollama")
    suspend fun ollama(): ResponseEntity<Any> = component("ollama")

    @GetMapping("/database")
    suspend fun database(): ResponseEntity<Any> = component("database")

    @GetMapping("/cache")
    suspend fun cache(): ResponseEntity<Any> = component("cache")
}
