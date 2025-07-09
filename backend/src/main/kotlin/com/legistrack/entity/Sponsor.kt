package com.legistrack.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sponsors")
data class Sponsor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bioguide_id", unique = true, nullable = false, length = 20)
    val bioguideId: String,

    @Column(name = "first_name", length = 100)
    val firstName: String? = null,

    @Column(name = "last_name", length = 100)
    val lastName: String? = null,

    @Column(length = 10)
    val party: String? = null,

    @Column(length = 2)
    val state: String? = null,

    @Column(length = 10)
    val district: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "sponsor", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val documentSponsors: List<DocumentSponsor> = emptyList()
)
