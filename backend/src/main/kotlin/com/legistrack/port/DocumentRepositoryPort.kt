package com.legistrack.port

import com.legistrack.dto.DocumentBasicDto
import com.legistrack.entity.AiAnalysis
import com.legistrack.entity.Document
import com.legistrack.entity.DocumentAction
import com.legistrack.entity.DocumentSponsor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

/**
 * Port interface for document repository operations.
 *
 * Defines the contract for document persistence without coupling
 * to specific persistence technology or implementation details.
 */
interface DocumentRepositoryPort {
    /**
     * Saves a document to the repository.
     *
     * @param document Document to save
     * @return Saved document with generated ID
     */
    fun save(document: Document): Document

    /**
     * Saves multiple documents to the repository.
     *
     * @param documents Documents to save
     * @return Saved documents with generated IDs
     */
    fun saveAll(documents: List<Document>): List<Document>

    /**
     * Finds a document by its unique identifier.
     *
     * @param id Document ID
     * @return Document if found, null otherwise
     */
    fun findById(id: Long): Document?

    /**
     * Finds a document by its bill ID.
     *
     * @param billId Congressional bill identifier
     * @return Document if found, null otherwise
     */
    fun findByBillId(billId: String): Document?

    /**
     * Checks if a document exists with the given bill ID.
     *
     * @param billId Congressional bill identifier
     * @return true if document exists, false otherwise
     */
    fun existsByBillId(billId: String): Boolean

    /**
     * Finds documents introduced after a specific date.
     *
     * @param date Cutoff date
     * @return List of documents introduced after the date
     */
    fun findByIntroductionDateAfter(date: LocalDate): List<Document>

    /**
     * Retrieves documents with pagination, ordered by introduction date.
     *
     * @param pageable Pagination parameters
     * @return Page of documents
     */
    fun findAllWithValidAnalyses(pageable: Pageable): Page<Document>

    /**
     * Searches documents by query string.
     *
     * @param query Search query
     * @param pageable Pagination parameters
     * @return Page of matching documents
     */
    fun searchDocuments(query: String, pageable: Pageable): Page<Document>

    /**
     * Finds documents by industry tag.
     *
     * @param tag Industry tag
     * @param pageable Pagination parameters
     * @return Page of documents with the industry tag
     */
    fun findByIndustryTag(tag: String, pageable: Pageable): Page<Document>

    /**
     * Finds basic document information by ID.
     *
     * @param id Document ID
     * @return Basic document data or null if not found
     */
    fun findDocumentBasicById(id: Long): DocumentBasicDto?

    /**
     * Finds document by ID with all related collections loaded.
     *
     * @param id Document ID
     * @return Document with details or null if not found
     */
    fun findByIdWithDetails(id: Long): Document?

    /**
     * Finds sponsors for a specific document.
     *
     * @param documentId Document ID
     * @return List of document sponsors
     */
    fun findSponsorsByDocumentId(documentId: Long): List<DocumentSponsor>

    /**
     * Finds actions for a specific document.
     *
     * @param documentId Document ID
     * @return List of document actions
     */
    fun findActionsByDocumentId(documentId: Long): List<DocumentAction>

    /**
     * Finds analyses for a specific document.
     *
     * @param documentId Document ID
     * @return List of AI analyses
     */
    fun findAnalysesByDocumentId(documentId: Long): List<AiAnalysis>

    /**
     * Counts documents that need AI analysis.
     *
     * @return Number of documents without valid analyses
     */
    fun countDocumentsNeedingAnalysis(): Long

    /**
     * Deletes a document by ID.
     *
     * @param id Document ID
     */
    fun deleteById(id: Long)

    /**
     * Deletes all documents.
     */
    fun deleteAll()

    /**
     * Counts total number of documents.
     *
     * @return Total document count
     */
    fun count(): Long

    /**
     * Checks if any documents exist.
     *
     * @return true if documents exist, false otherwise
     */
    fun existsAny(): Boolean
}