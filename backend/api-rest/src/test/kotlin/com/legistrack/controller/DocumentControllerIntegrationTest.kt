package com.legistrack.controller

import org.junit.jupiter.api.Test
import com.legistrack.domain.common.Page
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.ingestion.DataIngestionService
import com.legistrack.service.DocumentService
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.ResponseEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class DocumentControllerApiRestTest {
    private val documentService: DocumentService = mockk()
    private val dataIngestionService: DataIngestionService = mockk(relaxed = true)
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun should_returnEmptyPage_when_noDocumentsPresent() {
        // Mock empty page result
        val emptyPage = Page(
            content = emptyList<DocumentSummaryDto>(),
            totalElements = 0,
            totalPages = 0,
            pageNumber = 0,
            pageSize = 5,
            isFirst = true,
            isLast = true,
            hasNext = false,
            hasPrevious = false
        )
        val expectedPageable = PageRequest.of(0,5, Sort.by("introductionDate").descending())
        every { documentService.getAllDocuments(expectedPageable) } returns emptyPage
    val controller = DocumentsController(documentService, dataIngestionService, meterRegistry)
        val response: ResponseEntity<Any> = controller.getAllDocuments(0,5,"introductionDate","desc")
        assert(response.statusCode.is2xxSuccessful)
        val body = response.body as Page<*>
        assert(body.pageNumber == 0)
        assert(body.pageSize == 5)
        assert(body.totalElements == 0L)
        assert(body.content.isEmpty())
    }

    @Test
    fun should_returnError_when_pageNegative() {
    val controller = DocumentsController(documentService, dataIngestionService, meterRegistry)
        val response = controller.getAllDocuments(-1,5,"introductionDate","desc")
        assert(response.statusCode.value() == 400)
    val body = response.body as com.legistrack.api.ErrorResponse
    assert(!body.success)
    assert(body.message == "Page index must be >= 0")
    }

    @Test
    fun should_returnError_when_sizeOutOfRange() {
        val controller = DocumentsController(documentService, dataIngestionService, meterRegistry)
        val response = controller.getAllDocuments(0,0,"introductionDate","desc")
        assert(response.statusCode.value() == 400)
    val body = response.body as com.legistrack.api.ErrorResponse
    assert(body.message == "Size must be between 1 and 100")
    }

    @Test
    fun should_incrementSortRejectedMetric_when_invalidSortRequested() {
        val emptyPage = Page(
            content = emptyList<DocumentSummaryDto>(),
            totalElements = 0,
            totalPages = 0,
            pageNumber = 0,
            pageSize = 1,
            isFirst = true,
            isLast = true,
            hasNext = false,
            hasPrevious = false
        )
        every { documentService.getAllDocuments(any()) } returns emptyPage
        val controller = DocumentsController(documentService, dataIngestionService, meterRegistry)
        controller.getAllDocuments(0,1,"badField","asc")
        val count = meterRegistry.counter("congress.api.documents.sort.rejected", "requested", "badField").count().toInt()
        assert(count == 1) { "Expected sort rejection metric incremented once" }
    }
}
