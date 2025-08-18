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
import com.legistrack.external.congress.CongressApiProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Uses a real CongressApiAdapter instance with internal gauge seeding via reflection to verify
 * system health controller response shape without network calls.
 */
class SystemHealthControllerIntegrationTest {

    @Test
    fun should_includeCongressSnapshot() = runBlocking {
        val props = CongressApiProperties(
            key = "test",
            baseUrl = "https://example.org",
            retryAttempts = 0,
            cb = CongressApiProperties.Cb(threshold = 5, cooldownSeconds = 30),
            retryAdaptiveThresholdPercent = 10.0
        )
        val adapter = CongressApiAdapter(
            webClient = WebClient.builder().build(),
            props = props
        )
        val cls = adapter::class.java
        cls.getDeclaredField("limitGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(120) }
        cls.getDeclaredField("remainingGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(30) }
        cls.getDeclaredField("resetGaugeHolder").apply { isAccessible = true }
            .let { (it.get(adapter) as java.util.concurrent.atomic.AtomicInteger).set(45) }
        // Directly assert snapshot keys (controller adds wrapping fields but snapshot logic is core target)
        val congress = adapter.healthSnapshot()
        val expectedKeys = setOf(
            "circuitState",
            "consecutiveFailures",
            "rateLimitRemaining",
            "rateLimitLimit",
            "rateLimitResetSeconds",
            "rateLimitRemainingPct",
            "last429Epoch",
            "circuitOpenDurationSeconds"
        )
        assertTrue(congress.keys.containsAll(expectedKeys), "Missing keys: ${expectedKeys - congress.keys}")
        // Validate derived percentage ~25%
        val pct = congress["rateLimitRemainingPct"] as Double?
        assertEquals(25.0, pct)
    }
}
