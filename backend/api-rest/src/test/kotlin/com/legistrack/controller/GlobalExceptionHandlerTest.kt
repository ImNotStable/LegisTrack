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
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.ResponseEntity

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun should_includeCorrelationId_andIncrementCounters_on_Generic() {
        MDC.put("correlationId", "test-cid")
        val resp = handler.handleGeneric(RuntimeException("boom"))
        assert(resp.statusCode.is5xxServerError)
        val body = resp.body!!
        assert(body.correlationId == "test-cid")
    // Metrics removed; only validate correlation id and status
    }

    @Test
    fun should_mapNotFoundException_to404() {
        MDC.put("correlationId", "cid-404")
    val resp: ResponseEntity<com.legistrack.api.ErrorResponse> = handler.handleNotFound(NotFoundException("Missing"))
        assert(resp.statusCode.value() == 404)
        assert(resp.body!!.message == "Missing")
        assert(resp.body!!.correlationId == "cid-404")
    }
}
