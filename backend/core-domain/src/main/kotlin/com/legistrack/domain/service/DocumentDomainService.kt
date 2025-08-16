package com.legistrack.domain.service

import com.legistrack.domain.entity.Document
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.DocumentAction
import com.legistrack.domain.entity.DocumentSponsor

/**
 * Domain service interface for document-related business logic.
 * 
 * Contains pure business rules and domain operations that don't
 * belong in entities but are part of the domain layer.
 */
interface DocumentDomainService {
    /**
     * Validates a document before saving.
     * 
     * @param document The document to validate
     * @return List of validation errors, empty if valid
     */
    fun validateDocument(document: Document): List<String>

    /**
     * Determines if a document needs AI analysis.
     * 
     * @param document The document to check
     * @param existingAnalyses Current analyses for the document
     * @return true if analysis is needed, false otherwise
     */
    fun needsAnalysis(document: Document, existingAnalyses: List<AiAnalysis>): Boolean

    /**
     * Calculates the legislative progress score for a document.
     * 
     * @param document The document
     * @param actions List of actions taken on the document
     * @return Progress score from 0.0 to 1.0
     */
    fun calculateProgress(document: Document, actions: List<DocumentAction>): Double

    /**
     * Determines the primary sponsor from a list of sponsors.
     * 
     * @param sponsors List of document sponsors
     * @return The primary sponsor, or null if none found
     */
    fun findPrimarySponsor(sponsors: List<DocumentSponsor>): DocumentSponsor?

    /**
     * Validates an AI analysis before saving.
     * 
     * @param analysis The analysis to validate
     * @return List of validation errors, empty if valid
     */
    fun validateAnalysis(analysis: AiAnalysis): List<String>

    /**
     * Determines if a document is considered "stale" and needs updates.
     * 
     * @param document The document to check
     * @param actions Recent actions on the document
     * @return true if document is stale, false otherwise
     */
    fun isStale(document: Document, actions: List<DocumentAction>): Boolean
}