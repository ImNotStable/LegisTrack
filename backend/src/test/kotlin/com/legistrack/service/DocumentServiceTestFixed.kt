package com.legistrack.service

import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.DocumentRepositoryPort
import com.legistrack.domain.common.Page
import com.legistrack.domain.common.PageRequest as DomainPageRequest
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
import org.springframework.data.domain.PageRequest

/**
 * Unit tests for DocumentService.
 * Tests service logic, DTO conversion, and repository interactions.
 */
class DocumentServiceTestFixed {
    private lateinit var documentRepositoryPort: DocumentRepositoryPort
    private lateinit var documentService: DocumentService

    @BeforeEach
    fun setUp() {
        documentRepositoryPort = mockk()
        documentService = DocumentService(documentRepositoryPort)
    }

    @Test
    fun `should get all documents with pagination`() {
        val document = Document(
            id = 1L,
            billId = "HR1234-118",
            title = "Test Bill"
        )
        val page = Page(
            content = listOf(document),
            totalElements = 1,
            totalPages = 1,
            pageNumber = 0,
            pageSize = 10,
            isFirst = true,
            isLast = true,
            hasNext = false,
            hasPrevious = false
        )
        val springPageable = PageRequest.of(0, 10)
        
        every { documentRepositoryPort.findAllWithValidAnalyses(any()) } returns page
        every { documentRepositoryPort.findSponsorsByDocumentId(1L) } returns emptyList()
        every { documentRepositoryPort.findAnalysesByDocumentId(1L) } returns emptyList()

        val result = documentService.getAllDocuments(springPageable)

        assertEquals(1, result.content.size)
        assertEquals("HR1234-118", result.content[0].billId)
        assertEquals("Test Bill", result.content[0].title)
        verify { documentRepositoryPort.findAllWithValidAnalyses(any()) }
    }

    // TODO: Update remaining tests for Phase 2 architecture
}
