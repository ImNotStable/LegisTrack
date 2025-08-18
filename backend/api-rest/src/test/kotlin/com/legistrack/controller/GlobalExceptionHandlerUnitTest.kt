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

import com.legistrack.api.GlobalExceptionHandler
import com.legistrack.api.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.slf4j.MDC

/**
 * Focused unit test for GlobalExceptionHandler correlation behavior without metrics instrumentation.
 */
class GlobalExceptionHandlerUnitTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `records not found and generic exceptions`() {
        MDC.put("correlationId", "unit-cid-1")
        val notFoundResponse = handler.handleNotFound(NotFoundException("absent"))
        val genericResponse = handler.handleGeneric(RuntimeException("boom"))

        assertThat(notFoundResponse.body?.correlationId).isEqualTo("unit-cid-1")
        assertThat(genericResponse.body?.correlationId).isEqualTo("unit-cid-1")
    }

    @AfterEach
    fun cleanupMdc() { MDC.clear() }
}
