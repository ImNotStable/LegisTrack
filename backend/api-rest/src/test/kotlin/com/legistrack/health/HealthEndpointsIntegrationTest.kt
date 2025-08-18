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

package com.legistrack.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import com.legistrack.testsupport.PostgresTestContainerConfig

/**
 * Ensures consolidated and component health endpoints respond with 200 and expected JSON structure
 * after removal of metrics instrumentation.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [PostgresTestContainerConfig::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class HealthEndpointsIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should_returnAggregateHealth`() {
        webTestClient.get().uri("/api/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").exists()
            .jsonPath("$.components").exists()
    }

    @Test
    fun `should_returnComponentHealthSnapshots`() {
        // Explicit shortcut endpoints (mapped to internal component keys)
        // Some components may be DOWN in test env (e.g. external services), so only assert status presence.
        listOf("congress", "ollama").forEach { endpoint ->
            webTestClient.get().uri("/api/health/$endpoint")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").exists()
        }
    }
}
