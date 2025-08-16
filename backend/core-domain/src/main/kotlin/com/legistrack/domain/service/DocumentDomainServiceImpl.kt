package com.legistrack.domain.service

import com.legistrack.domain.entity.Document
import com.legistrack.domain.entity.AiAnalysis
import com.legistrack.domain.entity.DocumentAction
import com.legistrack.domain.entity.DocumentSponsor
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Default implementation of DocumentDomainService.
 */
class DocumentDomainServiceImpl : DocumentDomainService {

    override fun validateDocument(document: Document): List<String> {
        val errors = mutableListOf<String>()
        
        if (document.billId.isBlank()) {
            errors.add("Bill ID cannot be blank")
        }
        
        if (document.title.isBlank()) {
            errors.add("Title cannot be blank")
        }
        
        if (document.title.length > 1000) {
            errors.add("Title cannot exceed 1000 characters")
        }
        
        document.congressSession?.let { session ->
            if (session < 1 || session > 200) {
                errors.add("Congressional session must be between 1 and 200")
            }
        }
        
        return errors
    }

    override fun needsAnalysis(document: Document, existingAnalyses: List<AiAnalysis>): Boolean {
        // Document needs analysis if:
        // 1. No valid analyses exist
        // 2. Document was updated after the latest analysis
        val validAnalyses = existingAnalyses.filter { it.isValid }
        
        if (validAnalyses.isEmpty()) {
            return true
        }
        
        val latestAnalysis = validAnalyses.maxByOrNull { it.analysisDate }
        return latestAnalysis?.let { analysis ->
            document.updatedAt.isAfter(analysis.analysisDate)
        } ?: true
    }

    override fun calculateProgress(document: Document, actions: List<DocumentAction>): Double {
        if (actions.isEmpty()) return 0.0
        
        // Simple progress calculation based on action types
        val progressWeights = mapOf(
            "introduced" to 0.1,
            "referred" to 0.2,
            "markup" to 0.3,
            "reported" to 0.4,
            "passed" to 0.7,
            "signed" to 1.0,
            "enacted" to 1.0
        )
        
        val maxProgress = actions.mapNotNull { action ->
            progressWeights.entries.find { (key, _) ->
                action.actionText.lowercase().contains(key)
            }?.value
        }.maxOrNull() ?: 0.0
        
        return maxProgress
    }

    override fun findPrimarySponsor(sponsors: List<DocumentSponsor>): DocumentSponsor? {
        return sponsors.find { it.isPrimarySponsor }
    }

    override fun validateAnalysis(analysis: AiAnalysis): List<String> {
        val errors = mutableListOf<String>()
        
        if (analysis.generalEffectText.isNullOrBlank() && 
            analysis.economicEffectText.isNullOrBlank()) {
            errors.add("Analysis must have either general effect or economic effect text")
        }
        
        analysis.generalEffectText?.let { text ->
            if (text.length > 10000) {
                errors.add("General effect text cannot exceed 10,000 characters")
            }
        }
        
        analysis.economicEffectText?.let { text ->
            if (text.length > 10000) {
                errors.add("Economic effect text cannot exceed 10,000 characters")
            }
        }
        
        if (analysis.industryTags.size > 10) {
            errors.add("Cannot have more than 10 industry tags")
        }
        
        return errors
    }

    override fun isStale(document: Document, actions: List<DocumentAction>): Boolean {
        val daysSinceUpdate = ChronoUnit.DAYS.between(document.updatedAt.toLocalDate(), LocalDate.now())
        
        // Consider stale if:
        // 1. No updates in 30+ days and no recent actions
        if (daysSinceUpdate > 30) {
            val recentActions = actions.filter { action ->
                action.actionDate?.let { date ->
                    ChronoUnit.DAYS.between(date, LocalDate.now()) <= 30
                } ?: false
            }
            
            return recentActions.isEmpty()
        }
        
        return false
    }
}