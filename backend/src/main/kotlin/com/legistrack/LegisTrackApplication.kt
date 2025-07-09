package com.legistrack

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main application class for LegisTrack.
 * 
 * This Spring Boot application provides automated U.S. legislation tracking
 * and AI-powered analysis capabilities.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
class LegisTrackApplication

/**
 * Application entry point.
 */
fun main(args: Array<String>) {
    runApplication<LegisTrackApplication>(*args)
}
