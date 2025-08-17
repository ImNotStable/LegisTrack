package com.legistrack.api

/** Domain-level 404 to be mapped by GlobalExceptionHandler */
class NotFoundException(message: String): RuntimeException(message)
