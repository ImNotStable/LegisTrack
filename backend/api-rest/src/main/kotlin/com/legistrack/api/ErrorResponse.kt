package com.legistrack.api

/**
 * Standard API error envelope (success=false) per platform guideline.
 */
 data class ErrorResponse(
     val success: Boolean = false,
     val message: String,
     val details: Map<String, Any?>? = null,
     val correlationId: String? = null
 )
