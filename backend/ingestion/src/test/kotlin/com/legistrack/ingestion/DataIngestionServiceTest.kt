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

package com.legistrack.ingestion

import com.legistrack.domain.entity.Document
import com.legistrack.domain.port.CongressBillSummary
import com.legistrack.domain.port.CongressBillsPage
import com.legistrack.domain.port.CongressPort
import com.legistrack.domain.port.DocumentRepositoryPort
import com.legistrack.domain.entity.IngestionRun
import com.legistrack.domain.port.IngestionRunRepositoryPort
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DataIngestionServiceTest {

    private val congressPort: CongressPort = mockk()
    private val documentRepositoryPort: DocumentRepositoryPort = mockk(relaxed = true)
    private val ingestionRunRepositoryPort: IngestionRunRepositoryPort = mockk(relaxed = true)
    private val service = DataIngestionService(congressPort, documentRepositoryPort, ingestionRunRepositoryPort)

    @Test
    fun should_persistNewDocuments_when_recentBillsReturned() = runTest {
        val fromDate = LocalDate.now().minusDays(7)
        val bills = listOf(
            CongressBillSummary(congress = 118, number = "123", type = "HR", title = "Test Bill 1", introducedDate = null),
            CongressBillSummary(congress = 118, number = "456", type = "S", title = "Test Bill 2", introducedDate = null)
        )
    coEvery { congressPort.getRecentBills(fromDate, 0, 50) } returns CongressBillsPage(bills)
    every { ingestionRunRepositoryPort.findSuccessful(fromDate) } returns null
    every { ingestionRunRepositoryPort.create(fromDate) } returns IngestionRun(fromDate = fromDate, status = IngestionRun.Status.IN_PROGRESS)
        every { documentRepositoryPort.findByBillId(any()) } returns null
        every { documentRepositoryPort.save(any()) } answers { firstArg<Document>().copy(id = (1..100).random().toLong()) }

    val persisted = service.ingestRecentDocuments(fromDate)

        assertEquals(2, persisted)
    }

    @Test
    fun should_returnZero_when_congressPortFails() = runTest {
        val fromDate = LocalDate.now().minusDays(7)
    coEvery { congressPort.getRecentBills(any(), any(), any()) } throws RuntimeException("API down")
    every { ingestionRunRepositoryPort.findSuccessful(fromDate) } returns null
    every { ingestionRunRepositoryPort.create(fromDate) } returns IngestionRun(fromDate = fromDate, status = IngestionRun.Status.IN_PROGRESS)

        val persisted = service.ingestRecentDocuments(fromDate)

        assertEquals(0, persisted)
    }

    @Test
    fun should_skipExistingDocument_when_duplicateBillIdReturned() = runTest {
        val fromDate = LocalDate.now().minusDays(7)
        val bill = CongressBillSummary(congress = 118, number = "789", type = "HR", title = "Existing Bill", introducedDate = null)
    coEvery { congressPort.getRecentBills(fromDate, 0, 50) } returns CongressBillsPage(listOf(bill))
    every { ingestionRunRepositoryPort.findSuccessful(fromDate) } returns null
    every { ingestionRunRepositoryPort.create(fromDate) } returns IngestionRun(fromDate = fromDate, status = IngestionRun.Status.IN_PROGRESS)
        every { documentRepositoryPort.findByBillId("HR789-118") } returns Document(billId = "HR789-118", title = "Existing Bill", congressSession = 118, billType = "HR")

        val persisted = service.ingestRecentDocuments(fromDate)

        assertEquals(0, persisted)
    }
    @Test
    fun should_skipIngestion_when_successfulRunExists() = runTest {
        val fromDate = LocalDate.now().minusDays(7)
        every { ingestionRunRepositoryPort.findSuccessful(fromDate) } returns IngestionRun(id = 10, fromDate = fromDate, status = IngestionRun.Status.SUCCESS)
        val persisted = service.ingestRecentDocuments(fromDate)
        assertEquals(0, persisted)
    // Metrics removed; ensure skip still returns zero without error
    assertTrue(persisted == 0)
    }
}
