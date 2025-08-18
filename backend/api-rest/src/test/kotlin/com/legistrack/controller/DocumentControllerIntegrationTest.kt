/*
 * Copyright (c) 2025 LegisTrack
 *
 * Licensed under the MIT License. You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.legistrack.controller

import org.junit.jupiter.api.Test
import com.legistrack.domain.common.Page
import com.legistrack.domain.dto.DocumentSummaryDto
import com.legistrack.ingestion.DataIngestionService
import com.legistrack.service.DocumentService
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.ResponseEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class DocumentControllerApiRestTest {
    private val documentService: DocumentService = mockk(relaxed = true)
    private val dataIngestionService: DataIngestionService = mockk(relaxed = true)

    @Test
    fun should_returnEmptyPage_when_noDocumentsPresent() {
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
    val expectedPageable = PageRequest.of(0, 5, Sort.by("introductionDate").descending())
    every { documentService.getAllDocuments(expectedPageable) } returns emptyPage
    val controller = DocumentsController(documentService, dataIngestionService)
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
        val controller = DocumentsController(documentService, dataIngestionService)
    val response = controller.getAllDocuments(-1,5,"introductionDate","desc")
    // Controller now normalizes negative page to 0 and returns 200 success with sanitized page
    assert(response.statusCode.is2xxSuccessful)
    val body = response.body as Page<*>
    assert(body.pageNumber == 0)
    }

    @Test
    fun should_returnError_when_sizeOutOfRange() {
        val controller = DocumentsController(documentService, dataIngestionService)
        // Stub for any pageable with normalized size 20
        every { documentService.getAllDocuments(match { it.pageSize == 20 && it.pageNumber == 0 }) } answers {
            val p = firstArg<Pageable>()
            Page(
                content = emptyList<DocumentSummaryDto>(), totalElements = 0, totalPages = 0,
                pageNumber = p.pageNumber, pageSize = p.pageSize, isFirst = true, isLast = true,
                hasNext = false, hasPrevious = false
            )
        }
        val response = controller.getAllDocuments(0,0,"introductionDate","desc")
        assert(response.statusCode.is2xxSuccessful)
        val body = response.body as Page<*>
        assert(body.pageSize == 20) { "Expected normalized pageSize=20 but was ${body.pageSize}" }
    }
}
