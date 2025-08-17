package com.legistrack.domain.port

import com.legistrack.domain.entity.AiAnalysis

/**
 * Port interface for AI analysis repository operations.
 *
 * Defines the contract for AI analysis persistence without coupling
 * to specific persistence technology or implementation details.
 */
interface AiAnalysisRepositoryPort {
    /**
     * Saves an AI analysis to the repository.
     *
     * @param analysis AI analysis to save
     * @return Saved AI analysis with generated ID
     */
    fun save(analysis: AiAnalysis): AiAnalysis

    /**
     * Saves multiple AI analyses to the repository.
     *
     * @param analyses AI analyses to save
     * @return Saved AI analyses with generated IDs
     */
    fun saveAll(analyses: List<AiAnalysis>): List<AiAnalysis>

    /**
     * Finds an AI analysis by its unique identifier.
     *
     * @param id AI analysis ID
     * @return AI analysis if found, null otherwise
     */
    fun findById(id: Long): AiAnalysis?

    /**
     * Finds all valid AI analyses for a specific document.
     *
     * @param documentId Document ID
     * @return List of valid AI analyses ordered by creation date descending
     */
    fun findValidByDocumentId(documentId: Long): List<AiAnalysis>

    /**
     * Finds the latest valid AI analysis for a specific document.
     *
     * @param documentId Document ID
     * @return Latest valid AI analysis if found, null otherwise
     */
    fun findLatestValidByDocumentId(documentId: Long): AiAnalysis?

    /**
     * Finds all AI analyses for a specific document (including invalid ones).
     *
     * @param documentId Document ID
     * @return List of all AI analyses ordered by creation date descending
     */
    fun findAllByDocumentId(documentId: Long): List<AiAnalysis>

    /**
     * Checks if a document has any valid AI analyses.
     *
     * @param documentId Document ID
     * @return true if document has valid analyses, false otherwise
     */
    fun hasValidAnalyses(documentId: Long): Boolean

    /**
     * Marks an AI analysis as invalid.
     *
     * @param id AI analysis ID
     * @return true if analysis was found and updated, false otherwise
     */
    fun markAsInvalid(id: Long): Boolean

    /**
     * Deletes an AI analysis by ID.
     *
     * @param id AI analysis ID
     */
    fun deleteById(id: Long)

    /**
     * Deletes all AI analyses for a specific document.
     *
     * @param documentId Document ID
     */
    fun deleteAllByDocumentId(documentId: Long)

    /**
     * Counts total number of AI analyses.
     *
     * @return Total AI analysis count
     */
    fun count(): Long

    /**
     * Counts valid AI analyses.
     *
     * @return Valid AI analysis count
     */
    fun countValid(): Long
}