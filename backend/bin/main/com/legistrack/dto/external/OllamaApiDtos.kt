package com.legistrack.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// Ollama API DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val options: OllamaOptions? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaGenerateResponse(
    val model: String? = null,
    val response: String? = null,
    val done: Boolean = false,
    val context: List<Int>? = null,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Int? = null,
    val promptEvalDuration: Long? = null,
    val evalCount: Int? = null,
    val evalDuration: Long? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaOptions(
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val numCtx: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaTagsResponse(
    val models: List<OllamaModel> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaModel(
    val name: String,
    val modifiedAt: String? = null,
    val size: Long? = null,
    val digest: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaPullRequest(
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaPullResponse(
    val status: String? = null,
    val digest: String? = null,
    val total: Long? = null,
    val completed: Long? = null,
)
