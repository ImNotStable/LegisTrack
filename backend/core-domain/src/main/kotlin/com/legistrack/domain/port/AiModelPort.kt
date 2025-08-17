package com.legistrack.domain.port

/**
 * Port interface for AI model operations.
 * Provides abstraction over underlying AI provider (e.g., Ollama).
 */
interface AiModelPort {
    fun isServiceReady(): Boolean
    suspend fun isModelAvailable(): Boolean
    suspend fun generateAnalysis(prompt: String, temperature: Double = 0.2): String?
    suspend fun generateGeneralEffectAnalysis(billTitle: String, billSummary: String?): String?
    suspend fun generateEconomicEffectAnalysis(billTitle: String, billSummary: String?): String?
    suspend fun generateIndustryTags(billTitle: String, billSummary: String?): List<String>
}