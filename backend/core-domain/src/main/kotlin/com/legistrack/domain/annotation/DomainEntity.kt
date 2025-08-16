package com.legistrack.domain.annotation

/**
 * Annotation to mark domain entities.
 * Used by NoArg plugin to generate no-argument constructors for data classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DomainEntity