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

import com.legistrack.external.congress.CongressApiAdapter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Duplicate of SystemHealthControllerTest retained temporarily; disabled and renamed.
 */
@Disabled("Duplicate of SystemHealthControllerTest; pending deletion once git history cleaned")
class SystemHealthControllerDuplicateToRemove {
    private val congressApiAdapter: CongressApiAdapter = mockk(relaxed = true)
    private val healthCheckService: HealthCheckService = mockk(relaxed = true)
    private val controller = SystemHealthController(congressApiAdapter, healthCheckService)

    @Test
    fun `should_returnSystemHealth_withCorrelationAndCongressSnapshot`() = runBlocking {
        val cid = "sys-health-corr-1"
        val congressSnapshot = mapOf(
            "circuitState" to "CLOSED",
            "consecutiveFailures" to 0,
            "rateLimitRemaining" to 50,
            "rateLimitLimit" to 100,
            "rateLimitResetSeconds" to 30,
            "rateLimitRemainingPct" to 50.0,
            "last429Epoch" to null,
            "circuitOpenDurationSeconds" to 0.0
        )
        every { congressApiAdapter.healthSnapshot() } returns congressSnapshot
        coEvery { healthCheckService.health() } returns AggregateHealth(
            success = true,
            status = "UP",
            components = emptyMap(),
            correlationId = cid
        )
        val resp = controller.systemHealth()
        assertThat(resp.statusCode.value()).isEqualTo(200)
        val body = resp.body as SystemHealthResponse
        assertThat(body.success).isTrue()
        assertThat(body.status).isEqualTo("UP")
        assertThat(body.correlationId).isEqualTo(cid)
        assertThat(body.congress["circuitState"]).isEqualTo("CLOSED")
        assertThat(body.congress["rateLimitRemaining"]).isEqualTo(50)
        assertThat(body.congress["rateLimitRemainingPct"]).isEqualTo(50.0)
    }
}
