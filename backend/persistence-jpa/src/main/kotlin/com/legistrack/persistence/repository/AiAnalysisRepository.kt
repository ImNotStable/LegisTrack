package com.legistrack.persistence.repository

import com.legistrack.persistence.entity.AiAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AiAnalysisRepository : JpaRepository<AiAnalysis, Long> {
    fun findByDocumentId(documentId: Long): List<AiAnalysis>

    fun findByDocumentIdOrderByCreatedAtDesc(documentId: Long): List<AiAnalysis>

    fun findByDocumentIdAndIsValid(
        documentId: Long,
        isValid: Boolean,
    ): List<AiAnalysis>

    fun findByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId: Long): List<AiAnalysis>

    fun findFirstByDocumentIdAndIsValidTrueOrderByCreatedAtDesc(documentId: Long): AiAnalysis?

    @Modifying
    @Query("UPDATE AiAnalysis a SET a.isValid = false WHERE a.id = :analysisId")
    fun invalidateAnalysis(analysisId: Long): Int

    fun existsByDocumentIdAndIsValid(
        documentId: Long,
        isValid: Boolean,
    ): Boolean

    fun existsByDocumentIdAndIsValidTrue(documentId: Long): Boolean

    fun deleteByDocumentId(documentId: Long)

    fun countByIsValidTrue(): Long

    @Query("SELECT COUNT(DISTINCT a.document.id) FROM AiAnalysis a WHERE a.isValid = true")
    fun countDocumentsWithValidAnalysis(): Long
}
