package com.legistrack.domain.entity

import com.legistrack.domain.annotation.DomainEntity
import java.time.LocalDateTime

/**
 * Domain entity representing an AI analysis of a legislative document.
 *
 * @property id Unique identifier for the analysis
 * @property documentId Reference to the document this analysis belongs to
 * @property generalEffectText AI-generated general effect summary
 * @property economicEffectText AI-generated economic impact analysis
 * @property industryTags List of industry tags identified by AI
 * @property isValid Whether this analysis is considered valid
 * @property analysisDate When the analysis was performed
 * @property modelUsed Name/version of the AI model used
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp when record was last updated
 */
@DomainEntity
data class AiAnalysis(
    val id: Long? = null,
    val documentId: Long,
    val generalEffectText: String? = null,
    val economicEffectText: String? = null,
    val industryTags: List<String> = emptyList(),
    val isValid: Boolean = true,
    val analysisDate: LocalDateTime = LocalDateTime.now(),
    val modelUsed: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /**
     * Checks if this analysis has substantial content.
     */
    fun hasSubstantialContent(): Boolean = 
        !generalEffectText.isNullOrBlank() || !economicEffectText.isNullOrBlank()

    /**
     * Checks if this analysis has industry tags.
     */
    fun hasIndustryTags(): Boolean = industryTags.isNotEmpty()

    /**
     * Creates a copy marked as invalid.
     */
    fun markAsInvalid(): AiAnalysis = copy(
        isValid = false,
        updatedAt = LocalDateTime.now()
    )

    /**
     * Creates a copy with updated content.
     */
    fun withUpdatedContent(
        generalEffect: String? = null,
        economicEffect: String? = null,
        tags: List<String>? = null,
        modelUsed: String? = null
    ): AiAnalysis = copy(
        generalEffectText = generalEffect ?: this.generalEffectText,
        economicEffectText = economicEffect ?: this.economicEffectText,
        industryTags = tags ?: this.industryTags,
        modelUsed = modelUsed ?: this.modelUsed,
        updatedAt = LocalDateTime.now()
    )

    /**
     * Returns a summary of the analysis content.
     */
    val contentSummary: String
        get() = buildString {
            if (!generalEffectText.isNullOrBlank()) append("General: Yes, ")
            if (!economicEffectText.isNullOrBlank()) append("Economic: Yes, ")
            if (industryTags.isNotEmpty()) append("Tags: ${industryTags.size}, ")
            if (isEmpty()) append("No content")
            else setLength(length - 2) // Remove trailing ", "
        }
}