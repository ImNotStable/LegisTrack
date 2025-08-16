package com.legistrack.domain.common

/**
 * Domain-specific page result.
 * 
 * Framework-agnostic replacement for Spring's Page interface.
 */
data class Page<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    val isEmpty: Boolean get() = content.isEmpty()
    val isNotEmpty: Boolean get() = content.isNotEmpty()
    
    fun <U> map(transform: (T) -> U): Page<U> = Page(
        content = content.map(transform),
        totalElements = totalElements,
        totalPages = totalPages,
        pageNumber = pageNumber,
        pageSize = pageSize,
        isFirst = isFirst,
        isLast = isLast,
        hasNext = hasNext,
        hasPrevious = hasPrevious
    )
    
    companion object {
        fun <T> empty(): Page<T> = Page(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            pageNumber = 0,
            pageSize = 0,
            isFirst = true,
            isLast = true,
            hasNext = false,
            hasPrevious = false
        )
    }
}