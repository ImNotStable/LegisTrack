package com.legistrack.controller

import com.legistrack.domain.dto.DocumentSummaryDto
// TODO: Phase 3 - Re-enable DataIngestionService tests
// import com.legistrack.service.DataIngestionService
import com.legistrack.service.DocumentService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
// Removed deprecated SpyBean usage
import com.legistrack.domain.common.Page
import com.legistrack.domain.dto.PartyBreakdownDto
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
// Removed unused date imports

@WebMvcTest(controllers = [DocumentController::class])
class DocumentControllerTest {
	@Autowired
	lateinit var mockMvc: MockMvc

	@MockkBean
	lateinit var documentService: DocumentService

	// TODO: Phase 3 - Re-enable DataIngestionService tests
	// @MockkBean
	// lateinit var dataIngestionService: DataIngestionService

	@Test
	fun `getAllDocuments clamps negative page and large size`() {
		val pageableSlot: CapturingSlot<Pageable> = slot()
		every { documentService.getAllDocuments(capture(pageableSlot)) } answers {
			Page(
				content = emptyList<DocumentSummaryDto>(),
				totalElements = 0,
				totalPages = 0,
				pageNumber = 0,
				pageSize = 20,
				isFirst = true,
				isLast = true,
				hasNext = false,
				hasPrevious = false
			)
		}

		mockMvc.perform(get("/api/documents?page=-5&size=500&sortBy=introductionDate&sortDir=desc"))
			.andExpect(status().isOk)

		verify { documentService.getAllDocuments(any()) }
		assertEquals(0, pageableSlot.captured.pageNumber)
		assertEquals(100, pageableSlot.captured.pageSize)
	}

	@Test
	fun `analyzeDocument error returns standardized envelope`() {
		coEvery { documentService.analyzeDocument(123L) } throws RuntimeException("boom")

		mockMvc.perform(post("/api/documents/123/analyze").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is5xxServerError)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""{"success":false,"message":"boom"}"""))
	}
}


