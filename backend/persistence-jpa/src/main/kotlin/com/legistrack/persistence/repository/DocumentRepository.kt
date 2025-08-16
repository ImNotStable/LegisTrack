package com.legistrack.persistence.repository

import com.legistrack.domain.dto.DocumentBasicDto
import com.legistrack.persistence.entity.AiAnalysis
import com.legistrack.persistence.entity.Document
import com.legistrack.persistence.entity.DocumentAction
import com.legistrack.persistence.entity.DocumentSponsor
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
     * Retrieves document IDs with pagination, avoiding collection fetch warnings.
     *
     * This method only fetches document IDs with basic info, no relationships.
     *
     * @param pageable Pagination parameters
     * @return Page of documents without relationships loaded
     */
    @Query(
        """
        SELECT d FROM Document d 
        ORDER BY d.introductionDate DESC
    """,
    )
    fun findAllWithValidAnalyses(pageable: Pageable): Page<Document>

    /**
     * Full-text like search across title, official summary, and billId.
     */
    @Query(
        """
        SELECT d FROM Document d
        WHERE lower(d.title) LIKE lower(concat('%', :query, '%'))
           OR lower(d.officialSummary) LIKE lower(concat('%', :query, '%'))
           OR lower(d.billId) LIKE lower(concat('%', :query, '%'))
        ORDER BY d.introductionDate DESC
        """
    )
    fun searchDocuments(
        @Param("query") query: String,
        pageable: Pageable,
    ): Page<Document>

    /**
     * Find documents by industry tag using Postgres ANY on text[] column.
     */
    @Query(
        value = """
        SELECT d.*
        FROM documents d
        JOIN ai_analyses a ON a.document_id = d.id
        WHERE a.is_valid = true
          AND :tag = ANY(a.industry_tags)
        ORDER BY d.introduction_date DESC
        """,
        countQuery = """
        SELECT count(DISTINCT d.id)
        FROM documents d
        JOIN ai_analyses a ON a.document_id = d.id
        WHERE a.is_valid = true
          AND :tag = ANY(a.industry_tags)
        """,
        nativeQuery = true,
    )
    fun findByIndustryTag(
        @Param("tag") tag: String,
        pageable: Pageable,
    ): Page<Document>

    /**
     * Finds a document by ID with only scalar fields, no collections.
     * Returns a projection DTO to completely avoid entity collections.
     *
     * @param id Document ID
     * @return Basic document data or null if not found
     */
    @Query(
        """
        SELECT new com.legistrack.domain.dto.DocumentBasicDto(
            d.id, d.billId, d.title, d.officialSummary, d.introductionDate,
            d.congressSession, d.billType, d.fullTextUrl, d.status,
            d.createdAt, d.updatedAt
        )
        FROM Document d 
        WHERE d.id = :id
    """,
    )
    fun findDocumentBasicById(
        @Param("id") id: Long,
    ): DocumentBasicDto?

    /**
     * Gets sponsors for a specific document.
     */
    @Query(
        """
        SELECT ds FROM DocumentSponsor ds 
        JOIN FETCH ds.sponsor 
        WHERE ds.document.id = :documentId
    """,
    )
    fun findSponsorsByDocumentId(
        @Param("documentId") documentId: Long,
    ): List<DocumentSponsor>

    /**
     * Gets actions for a specific document.
     */
    @Query("SELECT da FROM DocumentAction da WHERE da.document.id = :documentId")
    fun findActionsByDocumentId(
        @Param("documentId") documentId: Long,
    ): List<DocumentAction>

    /**
     * Gets analyses for a specific document.
     */
    @Query("SELECT aa FROM AiAnalysis aa WHERE aa.document.id = :documentId")
    fun findAnalysesByDocumentId(
        @Param("documentId") documentId: Long,
    ): List<AiAnalysis>

    /**
     * Counts documents that need AI analysis.
     *
     * @return Number of documents without valid analyses
     */
    @Query(
        """
        SELECT COUNT(d) FROM Document d 
        WHERE d.id NOT IN (
            SELECT a.document.id FROM AiAnalysis a 
            WHERE a.isValid = true
        )
    """,
    )
    fun countDocumentsNeedingAnalysis(): Long

    /**
     * Finds a document by ID and eagerly fetches related collections to avoid N+1 queries.
     */
    @Query(
        """
        SELECT d FROM Document d
        WHERE d.id = :id
        """,
    )
    fun findByIdWithDetails(@Param("id") id: Long): Document?
}
