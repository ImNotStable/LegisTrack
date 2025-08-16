package com.legistrack.domain.common

/**
 * Domain-specific pagination request.
 * 
 * Framework-agnostic replacement for Spring's Pageable interface.
 */
data class PageRequest(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: Sort? = null
) {
    companion object {
        fun of(page: Int, size: Int): PageRequest = PageRequest(page, size)
        fun of(page: Int, size: Int, sort: Sort): PageRequest = PageRequest(page, size, sort)
    }
}

/**
 * Domain-specific sort specification.
 */
data class Sort(
    val orders: List<Order>
) {
    data class Order(
        val property: String,
        val direction: Direction
    )
    
    enum class Direction {
        ASC, DESC
    }
    
    companion object {
        fun by(vararg properties: String): Sort = Sort(
            properties.map { Order(it, Direction.ASC) }
        )
        
        fun by(direction: Direction, vararg properties: String): Sort = Sort(
            properties.map { Order(it, direction) }
        )
    }
}