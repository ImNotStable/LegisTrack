package com.legistrack.controller

import com.legistrack.service.external.LegislativeDataService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST Controller for legislative data operations.
 * Provides endpoints for searching and retrieving comprehensive bill information
 * from multiple government data sources.
 */
@RestController
@RequestMapping("/api/legislative")
class LegislativeApiController(
    private val legislativeDataService: LegislativeDataService,
) {
    private val logger = LoggerFactory.getLogger(LegislativeApiController::class.java)

    /**
     * Get comprehensive bill details by congress, type, and number
     */
    @GetMapping("/bills/{congress}/{type}/{number}")
    suspend fun getBillDetails(
        @PathVariable congress: Int,
        @PathVariable type: String,
        @PathVariable number: String,
    ): ResponseEntity<Map<String, Any>> =
        try {
            logger.info("Fetching bill details for $type$number in Congress $congress")

            val billDetails = legislativeDataService.getBillDetails(congress, type, number)

            if (billDetails != null) {
                ResponseEntity.ok(
                    mapOf<String, Any>(
                        "success" to true,
                        "data" to billDetails,
                    ),
                )
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch bill details for $type$number: ${e.message}", e)
            ResponseEntity.status(500).body(
                mapOf<String, Any>(
                    "success" to false,
                    "message" to "Internal server error while fetching bill details",
                ),
            )
        }

    /**
     * Search for bills across multiple data sources
     */
    @GetMapping("/bills/search")
    suspend fun searchBills(
        @RequestParam query: String,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<Map<String, Any>> {
        return try {
            logger.info("Searching bills with query: '$query', limit: $limit")

            if (query.isBlank()) {
                return ResponseEntity.badRequest().body(
                    mapOf<String, Any>(
                        "success" to false,
                        "message" to "Query parameter cannot be empty",
                    ),
                )
            }

            val clampedLimit = limit.coerceIn(1, 100)
            val searchResults = legislativeDataService.searchBills(query, clampedLimit)

            ResponseEntity.ok(
                mapOf<String, Any>(
                    "success" to true,
                    "data" to searchResults,
                    "totalResults" to searchResults.size,
                    "query" to query,
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to search bills with query '$query': ${e.message}", e)
            ResponseEntity.status(500).body(
                mapOf<String, Any>(
                    "success" to false,
                    "message" to "Internal server error while searching bills",
                ),
            )
        }
    }

    /**
     * Health check endpoint for legislative API services
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> =
        try {
            logger.debug("Legislative API health check requested")

            ResponseEntity.ok(
                mapOf<String, Any>(
                    "success" to true,
                    "service" to "LegislativeApiController",
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis(),
                ),
            )
        } catch (e: Exception) {
            logger.error("Health check failed: ${e.message}", e)
            ResponseEntity.status(500).body(
                mapOf<String, Any>(
                    "success" to false,
                    "service" to "LegislativeApiController",
                    "status" to "unhealthy",
                    "error" to (e.message ?: "Unknown error"),
                ),
            )
        }
}
