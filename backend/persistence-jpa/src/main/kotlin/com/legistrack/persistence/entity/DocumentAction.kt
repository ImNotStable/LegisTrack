package com.legistrack.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "document_actions")
data class DocumentAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,
    @Column(name = "action_date", nullable = false)
    val actionDate: LocalDate,
    @Column(name = "action_type", length = 100)
    val actionType: String? = null,
    @Column(name = "action_text", nullable = false, columnDefinition = "TEXT")
    val actionText: String,
    @Column(length = 20)
    val chamber: String? = null,
    @Column(name = "action_code", length = 50)
    val actionCode: String? = null,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
