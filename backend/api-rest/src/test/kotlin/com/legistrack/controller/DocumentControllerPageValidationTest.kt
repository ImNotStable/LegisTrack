package com.legistrack.controller

import com.legistrack.aianalysis.AiAnalysisService
import com.legistrack.domain.common.Page
import com.legistrack.domain.port.DocumentRepositoryPort
import com.legistrack.domain.entity.Document
import com.legistrack.service.DocumentService
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class DocumentControllerPageValidationTest {
    private val repo: DocumentRepositoryPort = mockk(relaxed = true)
    private val ai: AiAnalysisService = mockk(relaxed = true)
    private val service = DocumentService(repo, ai)

    /**
     * Fake Pageable implementation allowing construction of invalid sizes (0, negative, > max)
     * so we can exercise the internal clamping logic without Spring throwing first.
     */
    private data class FakePageable(
        private val p: Int,
        private val s: Int,
        private val sortSpec: Sort = Sort.unsorted()
    ) : Pageable {
        override fun getPageNumber(): Int = p
        override fun getPageSize(): Int = s
        override fun getOffset(): Long = (if (p < 0) 0 else p) * (if (s < 0) 0 else s).toLong()
        override fun getSort(): Sort = sortSpec
        override fun next(): Pageable = copy(p = p + 1)
        override fun previousOrFirst(): Pageable = if (p == 0) this else copy(p = p - 1)
        override fun first(): Pageable = copy(p = 0)
        override fun withPage(pageNumber: Int): Pageable = copy(p = pageNumber)
        override fun hasPrevious(): Boolean = p > 0
    }

    private fun emptyRepoPage(docPageSize: Int): Page<Document> = Page(
        content = emptyList(),
        totalElements = 0,
        totalPages = 0,
        pageNumber = 0,
        pageSize = docPageSize,
        isFirst = true,
        isLast = true,
        hasNext = false,
        hasPrevious = false
    )

    @Test
    fun `should clamp excessive page size`() {
        val captured = slot<com.legistrack.domain.common.PageRequest>()
        every { repo.findAllWithValidAnalyses(capture(captured)) } answers { emptyRepoPage(captured.captured.pageSize) }
        val oversized = FakePageable(0, 5000)
        val page = service.getAllDocuments(oversized)
        // Validate clamping applied in service before repository call
        assertThat(captured.captured.pageSize).isEqualTo(100)
        assertThat(page.pageSize).isEqualTo(100)
    }

    @Test
    fun `should default invalid page size to 20`() {
        val captured = slot<com.legistrack.domain.common.PageRequest>()
        every { repo.findAllWithValidAnalyses(capture(captured)) } answers { emptyRepoPage(captured.captured.pageSize) }
        val zeroSize = FakePageable(0, 0)
        val page = service.getAllDocuments(zeroSize)
        assertThat(captured.captured.pageSize).isEqualTo(20)
        assertThat(page.pageSize).isEqualTo(20)
    }

    @Test
    fun `should coerce negative page number to zero`() {
        val captured = slot<com.legistrack.domain.common.PageRequest>()
        every { repo.findAllWithValidAnalyses(capture(captured)) } answers { emptyRepoPage(captured.captured.pageSize) }
        val negativePage = FakePageable(-5, 10)
        val page = service.getAllDocuments(negativePage)
        assertThat(captured.captured.pageNumber).isEqualTo(0)
        assertThat(page.pageNumber).isEqualTo(0)
    }
}
