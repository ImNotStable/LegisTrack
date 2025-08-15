package com.legistrack.controller

import com.legistrack.dto.DocumentDetailDto
import com.legistrack.dto.DocumentSummaryDto
import com.legistrack.dto.PartyBreakdownDto
import com.legistrack.service.DataIngestionService
import com.legistrack.service.DocumentService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [DocumentController::class])
class DocumentControllerTest {
	@Autowired
	lateinit var mockMvc: MockMvc

	@MockBean
	lateinit var documentService: DocumentService

	@MockBean
	lateinit var dataIngestionService: DataIngestionService

	@Test
	fun `getAllDocuments clamps negative page and large size`() {
		val pageableSlot = slot<Pageable>()
		every { documentService.getAllDocuments(capture(pageableSlot)) } answers {
			PageImpl(emptyList<DocumentSummaryDto>()) as Page<DocumentSummaryDto>
		}

		mockMvc.perform(get("/api/documents?page=-5&size=500&sortBy=introductionDate&sortDir=desc"))
			.andExpect(status().isOk)

		// Verify clamping: page -> 0, size -> 100
		assertEquals(0, pageableSlot.captured.pageNumber)
		assertEquals(100, pageableSlot.captured.pageSize)
	}

	@Test
	fun `analyzeDocument error returns standardized envelope`() {
		every { documentService.analyzeDocument(123L) } throws RuntimeException("boom")

		mockMvc.perform(post("/api/documents/123/analyze").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is5xxServerError)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("{"success":false,"message":"boom"}", true))
	}
}


