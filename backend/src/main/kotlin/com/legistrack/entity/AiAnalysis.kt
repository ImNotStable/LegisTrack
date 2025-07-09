package com.legistrack.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Entity representing an AI analysis of a legislative document.
 * 
 * @property id Unique identifier for the analysis
 * @property document The document this analysis belongs to
 * @property generalEffectText AI-generated general effect summary
 * @property economicEffectText AI-generated economic impact analysis
 * @property industryTags Array of industry tags identified by AI
 * @property isValid Whether this analysis is considered valid by users
 * @property analysisDate When the analysis was performed
 * @property modelUsed Name/version of the AI model used
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp when record was last updated
 */
@Entity
@Table(name = "ai_analyses")
data class AiAnalysis(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(name = "general_effect_text", columnDefinition = "TEXT")
    val generalEffectText: String? = null,

    @Column(name = "economic_effect_text", columnDefinition = "TEXT")
    val economicEffectText: String? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "industry_tags", columnDefinition = "text[]")
    val industryTags: Array<String> = emptyArray(),

    @Column(name = "is_valid")
    val isValid: Boolean = true,

    @Column(name = "analysis_date")
    val analysisDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "model_used", length = 100)
    val modelUsed: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AiAnalysis

        if (id != other.id) return false
        if (generalEffectText != other.generalEffectText) return false
        if (economicEffectText != other.economicEffectText) return false
        if (!industryTags.contentEquals(other.industryTags)) return false
        if (isValid != other.isValid) return false
        if (analysisDate != other.analysisDate) return false
        if (modelUsed != other.modelUsed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (generalEffectText?.hashCode() ?: 0)
        result = 31 * result + (economicEffectText?.hashCode() ?: 0)
        result = 31 * result + industryTags.contentHashCode()
        result = 31 * result + isValid.hashCode()
        result = 31 * result + analysisDate.hashCode()
        result = 31 * result + (modelUsed?.hashCode() ?: 0)
        return result
    }
}
