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

package com.legistrack.external.congress

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CongressApiPropertiesValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun should_fail_when_retryAttempts_negative() {
        val props = CongressApiProperties(
            key = "k",
            baseUrl = "https://example.com",
            retryAttempts = -1,
        )
        val violations = validator.validate(props)
        assertTrue(violations.any { it.propertyPath.toString() == "retryAttempts" }, "Expected violation on retryAttempts, got $violations")
    }

    @Test
    fun should_fail_when_baseUrl_invalid() {
        assertThrows(IllegalArgumentException::class.java) {
            CongressApiProperties(
                key = "k",
                baseUrl = "ftp://bad",
                retryAttempts = 0,
            )
        }
    }
}
