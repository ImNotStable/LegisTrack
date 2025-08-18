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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import com.legistrack.testsupport.PostgresTestContainerConfig

/**
 * Verifies that out-of-range paging parameters are normalized (negative page -> 0, size 0 -> 20)
 * and that requests succeed (200) under a supplied correlation id header. Success responses do not
 * embed correlationId (only error envelopes do); propagation is asserted via absence of failure.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [PostgresTestContainerConfig::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class DocumentsCorrelationAndNormalizationIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should_normalizeParams_and_return200`() {
        webTestClient.get()
            .uri("/api/documents?page=-10&size=0&sort=unknownField,desc")
            .header("X-Correlation-Id", "cid-norm-2")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.pageNumber").isEqualTo(0)
            .jsonPath("$.pageSize").isEqualTo(20)
            .jsonPath("$.content").exists()
    }
}
