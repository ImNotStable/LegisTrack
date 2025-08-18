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

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Forces early instantiation & validation of [CongressApiProperties] so that invalid
 * configuration (e.g. wrong URL scheme) fails fast during startup rather than lazily
 * when the adapter is first used. This preserves correctness after removal of metrics
 * and potential changes in property binding order. [CORR]
 */
@Component
class CongressApiPropertiesEagerValidator(private val props: CongressApiProperties) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun validate() {
        // Redundant with init{} guard + JSR 303, but ensures explicit early failure path.
        if (!(props.baseUrl.startsWith("http://") || props.baseUrl.startsWith("https://"))) {
            throw IllegalStateException("baseUrl must start with http/https")
        }
        logger.debug("CongressApiProperties validated (baseUrl={})", props.baseUrl)
    }
}
