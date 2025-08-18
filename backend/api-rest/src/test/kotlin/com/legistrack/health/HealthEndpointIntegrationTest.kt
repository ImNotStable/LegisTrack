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

import com.legistrack.domain.port.AiModelPort
import com.legistrack.domain.port.CongressBillsPage
import com.legistrack.domain.port.CongressPort
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import kotlinx.coroutines.runBlocking

@SpringBootTest(properties = [
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:health;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=none"
])
@ActiveProfiles("test")
@Import(TestHealthMocks::class)
class HealthEndpointIntegrationTest {
    @Autowired
    private lateinit var aiModelPort: AiModelPort

    @Autowired
    private lateinit var congressPort: CongressPort

    @Autowired
    private lateinit var healthCheckService: HealthCheckService

    @Test
    fun `health aggregate returns expected components`() = runBlocking {
        every { aiModelPort.isServiceReady() } returns true
        coEvery { aiModelPort.isModelAvailable() } returns true
        coEvery { congressPort.getRecentBills(any(), any(), any()) } returns CongressBillsPage()

        val result = healthCheckService.health()
        listOf("database", "cache", "ollama", "congressApi").forEach { key ->
            require(result.components.containsKey(key)) { "Missing component $key" }
            val comp = result.components[key]!!
            if (comp.status != "DOWN") {
                require(comp.lastSuccessEpochMs != null) { "Expected lastSuccessEpochMs for component $key when status=${comp.status}" }
            }
        }
        println("Health aggregate status=${result.status} success=${result.success} components=" +
            result.components.mapValues { it.value.status })
        // After new logic, cache issues should yield DEGRADED with success=true (critical components up)
        require(result.success) { "Expected success=true when critical components are up (status=${result.status})" }
        require(result.status in listOf("UP", "DEGRADED")) { "Unexpected aggregate status ${result.status}" }
    }
}

@Configuration
class TestHealthMocks {
    @Bean fun aiModelPort(): AiModelPort = mockk(relaxed = true)
    @Bean fun congressPort(): CongressPort = mockk(relaxed = true)
}
