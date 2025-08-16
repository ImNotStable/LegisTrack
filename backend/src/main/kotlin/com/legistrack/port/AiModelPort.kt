package com.legistrack.port

/**
 * Port interface for AI model operations.
 *
 * Defines the contract for AI analysis generation without coupling
 * to specific model providers or implementations.
 */
interface AiModelPort {
    /**
     * Checks if the AI model service is ready for analysis generation.
     *
     * @return true if service is ready, false otherwise
     */
    fun isServiceReady(): Boolean

    /**
     * Checks if the required AI model is available.
     *
     * @return true if model is available, false otherwise
     */
    suspend fun isModelAvailable(): Boolean

    /**
     * Generates AI analysis from a given prompt.
     *
     * @param prompt The analysis prompt
     * @return Generated analysis text or null if generation fails
     */
    suspend fun generateAnalysis(prompt: String): String?

    /**
     * Generates general effect analysis for a bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional summary of the bill
     * @return Generated general effect analysis or null if generation fails
     */
    suspend fun generateGeneralEffectAnalysis(
        billTitle: String,
        billSummary: String?,
    ): String?

    /**
     * Generates economic effect analysis for a bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional summary of the bill
     * @return Generated economic effect analysis or null if generation fails
     */
    suspend fun generateEconomicEffectAnalysis(
        billTitle: String,
        billSummary: String?,
    ): String?

    /**
     * Generates industry tags for a bill.
     *
     * @param billTitle Title of the bill
     * @param billSummary Optional summary of the bill
     * @return List of relevant industry tags
     */
    suspend fun generateIndustryTags(
        billTitle: String,
        billSummary: String?,
    ): List<String>
}