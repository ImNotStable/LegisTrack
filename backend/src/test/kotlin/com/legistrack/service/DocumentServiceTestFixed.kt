package com.legistrack.service

import com.legistrack.entity.Document
import com.legistrack.repository.AiAnalysisRepository
import com.legistrack.repository.DocumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/**
 * Unit tests for DocumentService.
 * Tests service logic, DTO conversion, and repository interactions.
 */
class DocumentServiceTestFixed {
    private lateinit var documentRepository: DocumentRepository
    private lateinit var aiAnalysisRepository: AiAnalysisRepository
    private lateinit var documentService: DocumentService

    @BeforeEach
    fun setUp() {
        documentRepository = mockk()
        aiAnalysisRepository = mockk()
        documentService = DocumentService(documentRepository, aiAnalysisRepository)
    }

    @Test
    fun `should get all documents with pagination`() {
        val document =
            Document(
                id = 1L,
                billId = "HR1234-118",
                title = "Test Bill",
            )
        val page = PageImpl(listOf(document))
        val pageable = PageRequest.of(0, 10)

        every { documentRepository.findAllWithValidAnalyses(pageable) } returns page

        val result = documentService.getAllDocuments(pageable)

        assertEquals(1, result.content.size)
        assertEquals("HR1234-118", result.content[0].billId)
        assertEquals("Test Bill", result.content[0].title)
        verify { documentRepository.findAllWithValidAnalyses(pageable) }
    }

    @Test
    fun `should get document by ID`() {
        val document =
            Document(
                id = 1L,
                billId = "HR1234-118",
                title = "Test Bill",
                officialSummary = "Test summary",
            )

        every { documentRepository.findByIdWithDetails(1L) } returns document

        val result = documentService.getDocumentById(1L)

        assertNotNull(result)
        assertEquals(1L, result?.id)
        assertEquals("HR1234-118", result?.billId)
        assertEquals("Test Bill", result?.title)
        assertEquals("Test summary", result?.officialSummary)
        verify { documentRepository.findByIdWithDetails(1L) }
    }

    @Test
    fun `should return null when document not found`() {
        every { documentRepository.findByIdWithDetails(999L) } returns null

        val result = documentService.getDocumentById(999L)

        assertNull(result)
        verify { documentRepository.findByIdWithDetails(999L) }
    }

    @Test
    fun `should invalidate analysis successfully`() {
        every { aiAnalysisRepository.invalidateAnalysis(1L) } returns 1

        val result = documentService.invalidateAnalysis(1L)

        assertTrue(result)
        verify { aiAnalysisRepository.invalidateAnalysis(1L) }
    }

    @Test
    fun `should fail to invalidate non-existent analysis`() {
        every { aiAnalysisRepository.invalidateAnalysis(999L) } returns 0

        val result = documentService.invalidateAnalysis(999L)

        assertFalse(result)
        verify { aiAnalysisRepository.invalidateAnalysis(999L) }
    }
}
