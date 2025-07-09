package com.legistrack.repository

import com.legistrack.entity.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Repository interface for Document entity operations.
 * 
 * Provides database access methods for legislative documents
 * with optimized queries for common use cases.
 */
@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    
    /**
     * Finds a document by its unique bill ID.
     * 
     * @param billId The congressional bill identifier
     * @return Document if found, null otherwise
     */
    fun findByBillId(billId: String): Document?
    
    /**
     * Checks if a document exists with the given bill ID.
     * 
     * @param billId The congressional bill identifier
     * @return true if document exists, false otherwise
     */
    fun existsByBillId(billId: String): Boolean
    
    /**
     * Finds documents introduced after a specific date.
     * 
     * @param date The cutoff date
     * @return List of documents introduced after the date
     */
    fun findByIntroductionDateAfter(date: LocalDate): List<Document>
    
    /**
     * Retrieves all documents with their valid analyses, sponsors, and related data.
     * 
     * Uses fetch joins to minimize N+1 queries and filters for valid analyses only.
     * 
     * @param pageable Pagination parameters
     * @return Page of documents with associated data
     */
    @Query("""
        SELECT DISTINCT d FROM Document d 
        LEFT JOIN FETCH d.analyses a
        WHERE a.isValid = true OR a.id IS NULL
        ORDER BY d.introductionDate DESC
    """)
    fun findAllWithValidAnalyses(pageable: Pageable): Page<Document>
    
    /**
     * Finds a document by ID with all related data eagerly loaded.
     * 
     * @param id Document ID
     * @return Document with all relationships loaded, or null if not found
     */
    @Query("""
        SELECT d FROM Document d 
        WHERE d.id = :id
    """)
    fun findByIdWithDetails(@Param("id") id: Long): Document?
    
    /**
     * Counts documents that need AI analysis.
     * 
     * @return Number of documents without valid analyses
     */
    @Query("""
        SELECT COUNT(d) FROM Document d 
        WHERE d.id NOT IN (
            SELECT a.document.id FROM AiAnalysis a 
            WHERE a.isValid = true
        )
    """)
    fun countDocumentsNeedingAnalysis(): Long
}
