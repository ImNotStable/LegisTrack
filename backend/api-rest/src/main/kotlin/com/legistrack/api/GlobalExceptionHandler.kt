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

package com.legistrack.api

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    // Metrics removed per requirement.

    private fun err(message: String, details: Map<String, Any?>? = null): ErrorResponse {
        val cid = MDC.get("correlationId")
        return ErrorResponse(message = message, details = details, correlationId = cid)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
    ResponseEntity.badRequest().body(err("Missing required parameter '${ex.parameterName}'"))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
    ResponseEntity.badRequest().body(err("Invalid value for parameter '${ex.name}'"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val msg = ex.bindingResult.fieldErrors.joinToString { "${it.field} ${it.defaultMessage}" }
    return ResponseEntity.badRequest().body(err(msg.ifBlank { "Validation failed" }))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(ex.message ?: "Not found"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(err("Internal server error"))
    }
}
