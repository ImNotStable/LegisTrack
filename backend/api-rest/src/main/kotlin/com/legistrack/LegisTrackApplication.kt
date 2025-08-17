package com.legistrack

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Spring Boot entrypoint (api-rest module) exposing REST controllers.
 * Other modules (domain, persistence, ingestion, ai-analysis, adapters) are pure libraries.
 */
@SpringBootApplication(scanBasePackages = [
    "com.legistrack"
])
@EnableCaching
@EnableAsync
@EnableScheduling
open class LegisTrackApplication

fun main(args: Array<String>) { runApplication<LegisTrackApplication>(*args) }
