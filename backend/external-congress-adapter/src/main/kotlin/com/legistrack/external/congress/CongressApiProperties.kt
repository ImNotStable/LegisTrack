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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@ConfigurationProperties(prefix = "app.congress.api")
@Validated
data class CongressApiProperties(
    @field:NotBlank
    var key: String = "dummy-test-key",
    @field:NotBlank
    @field:Pattern(regexp = "https?://.+", message = "baseUrl must start with http/https")
    var baseUrl: String = "https://api.congress.gov/v3",
    @field:Min(0)
    var retryAttempts: Long = 3,
    var cb: Cb = Cb(),
    @field:Min(0)
    @field:Max(100)
    var retryAdaptiveThresholdPercent: Double = 10.0
) {
    init {
        // Defensive guard to enforce scheme even if Bean Validation fails to trigger in certain startup paths. [CORR]
        if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            throw IllegalArgumentException("baseUrl must start with http/https")
        }
    }
    data class Cb(
        @field:Min(1)
        var threshold: Int = 5,
        @field:Min(1)
        var cooldownSeconds: Long = 30
    )
    val breakerThreshold: Int get() = cb.threshold
    val breakerCooldownSeconds: Long get() = cb.cooldownSeconds
}
