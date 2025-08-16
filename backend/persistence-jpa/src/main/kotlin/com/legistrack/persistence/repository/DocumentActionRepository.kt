package com.legistrack.persistence.repository

import com.legistrack.persistence.entity.DocumentAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Repository interface for DocumentAction entity operations.
 *
 * Provides database access methods for legislative actions
 * including chronological ordering and action type filtering.
 */
@Repository
interface DocumentActionRepository : JpaRepository<DocumentAction, Long> {
    /**
     * Finds all actions for a specific document ordered by action date descending.
     *
     * @param documentId The document ID
     * @return List of actions for the given document, newest first
     */
    fun findByDocumentIdOrderByActionDateDesc(documentId: Long): List<DocumentAction>

    /**
     * Finds the most recent action for a specific document.
     *
     * @param documentId The document ID
     * @return The most recent action if exists, null otherwise
     */
    fun findFirstByDocumentIdOrderByActionDateDesc(documentId: Long): DocumentAction?

    /**
     * Finds actions by document ID and action type.
     *
     * @param documentId The document ID
     * @param actionType The action type to filter by
     * @return List of actions matching the criteria
     */
    fun findByDocumentIdAndActionType(
        documentId: Long,
        actionType: String,
    ): List<DocumentAction>

    /**
     * Finds actions for a document within a date range.
     *
     * @param documentId The document ID
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @return List of actions within the date range
     */
    fun findByDocumentIdAndActionDateBetween(
        documentId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DocumentAction>

    /**
     * Checks if an action already exists for a document on a specific date with specific text.
     *
     * @param documentId The document ID
     * @param actionDate The action date
     * @param actionText The action text
     * @return true if action exists, false otherwise
     */
    fun existsByDocumentIdAndActionDateAndActionText(
        documentId: Long,
        actionDate: LocalDate,
        actionText: String,
    ): Boolean
}
