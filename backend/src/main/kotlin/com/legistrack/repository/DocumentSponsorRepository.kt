package com.legistrack.repository

import com.legistrack.entity.DocumentSponsor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for DocumentSponsor entity operations.
 * 
 * Provides database access methods for document sponsor relationships
 * including primary sponsor identification and sponsor history.
 */
@Repository
interface DocumentSponsorRepository : JpaRepository<DocumentSponsor, Long> {
    
    /**
     * Finds all sponsor relationships for a specific document.
     * 
     * @param documentId The document ID
     * @return List of document sponsors for the given document
     */
    fun findByDocumentId(documentId: Long): List<DocumentSponsor>
    
    /**
     * Finds the primary sponsor for a specific document.
     * 
     * @param documentId The document ID
     * @return The primary sponsor relationship if exists, null otherwise
     */
    fun findByDocumentIdAndIsPrimarySponsorTrue(documentId: Long): DocumentSponsor?
    
    /**
     * Checks if a sponsor relationship already exists for a document and sponsor.
     * 
     * @param documentId The document ID
     * @param sponsorId The sponsor ID
     * @return true if relationship exists, false otherwise
     */
    fun existsByDocumentIdAndSponsorId(documentId: Long, sponsorId: Long): Boolean
    
    /**
     * Finds all sponsorships for a specific sponsor.
     * 
     * @param sponsorId The sponsor ID
     * @return List of document sponsorships for the given sponsor
     */
    fun findBySponsorId(sponsorId: Long): List<DocumentSponsor>
}
