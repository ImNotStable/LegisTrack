package com.legistrack.config

import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import com.legistrack.external.congress.CongressApiProperties
import com.legistrack.external.ollama.OllamaProperties
import com.legistrack.ingestion.config.DataIngestionSchedulerProperties
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

/**
 * Enables stable DTO-based Page serialization to remove runtime warning about PageImpl JSON instability.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
@EnableConfigurationProperties(value = [CongressApiProperties::class, OllamaProperties::class, DataIngestionSchedulerProperties::class])
class WebConfig
