package com.legistrack.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Entity representing a U.S. legislative document.
 * 
 * @property id Unique identifier for the document
 * @property billId Congressional bill identifier (e.g., "HR1234-118")
 * @property title Official title of the legislation
 * @property officialSummary Official summary provided by Congress
 * @property introductionDate Date when the bill was introduced
 * @property congressSession Congressional session number
 * @property billType Type of bill (HR, S, etc.)
 * @property fullTextUrl URL to the full text of the document
 * @property status Current legislative status
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp when record was last updated
 * @property sponsors List of sponsors and co-sponsors
 * @property actions List of legislative actions taken on this document
 * @property analyses List of AI analyses performed on this document
 */
@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bill_id", unique = true, nullable = false, length = 50)
    val billId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "official_summary", columnDefinition = "TEXT")
    val officialSummary: String? = null,

    @Column(name = "introduction_date")
    val introductionDate: LocalDate? = null,

    @Column(name = "congress_session")
    val congressSession: Int? = null,

    @Column(name = "bill_type", length = 20)
    val billType: String? = null,

    @Column(name = "full_text_url", length = 500)
    val fullTextUrl: String? = null,

    @Column(length = 100)
    val status: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val sponsors: List<DocumentSponsor> = emptyList(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val actions: List<DocumentAction> = emptyList(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val analyses: List<AiAnalysis> = emptyList()
)
