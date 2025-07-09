package com.legistrack.service

import com.legistrack.dto.external.*
import com.legistrack.entity.*
import com.legistrack.repository.*
import com.legistrack.service.external.CongressApiService
import com.legistrack.service.external.OllamaService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional
class DataIngestionService(
    private val congressApiService: CongressApiService,
    private val ollamaService: OllamaService,
    private val documentRepository: DocumentRepository,
    private val sponsorRepository: SponsorRepository,
    private val aiAnalysisRepository: AiAnalysisRepository
) {
    
    private val logger = LoggerFactory.getLogger(DataIngestionService::class.java)
    
    suspend fun ingestRecentDocuments(fromDate: LocalDate = LocalDate.now().minusDays(1)): Int {
        logger.info("Starting data ingestion from date: {}", fromDate)
        
        var totalIngested = 0
        var offset = 0
        val limit = 20
        
        do {
            val response = congressApiService.getRecentBills(fromDate, offset, limit)
            val bills = response.bills
            
            logger.debug("Fetched {} bills at offset {}", bills.size, offset)
            
            for (bill in bills) {
                try {
                    if (processBill(bill)) {
                        totalIngested++
                    }
                } catch (e: Exception) {
                    logger.error("Error processing bill: ${bill.number}", e)
                }
            }
            
            offset += limit
        } while (bills.size == limit && offset < 200) // Limit to prevent runaway queries
        
        logger.info("Data ingestion completed. Total documents ingested: {}", totalIngested)
        return totalIngested
    }
    
    private suspend fun processBill(congressBill: CongressBill): Boolean {
        val billId = "${congressBill.type}${congressBill.number}-${congressBill.congress}"
        
        // Check if bill already exists
        if (documentRepository.existsByBillId(billId)) {
            logger.debug("Bill {} already exists, skipping", billId)
            return false
        }
        
        logger.info("Processing new bill: {}", billId)
        
        // Create and save document
        val document = createDocumentFromCongressBill(congressBill)
        val savedDocument = documentRepository.save(document)
        
        // Fetch and process detailed data
        congressBill.congress?.let { congress ->
            congressBill.type?.let { type ->
                congressBill.number?.let { number ->
                    processDetailedBillData(savedDocument, congress, type, number)
                }
            }
        }
        
        // Trigger AI analysis
        triggerAiAnalysis(savedDocument)
        
        return true
    }
    
    private fun createDocumentFromCongressBill(congressBill: CongressBill): Document {
        val billId = "${congressBill.type}${congressBill.number}-${congressBill.congress}"
        val introductionDate = congressBill.introducedDate?.let { 
            LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) 
        }
        
        return Document(
            billId = billId,
            title = congressBill.title ?: "No title available",
            officialSummary = congressBill.summaries.firstOrNull()?.text,
            introductionDate = introductionDate,
            congressSession = congressBill.congress,
            billType = congressBill.type,
            status = congressBill.latestAction?.text
        )
    }
    
    private suspend fun processDetailedBillData(document: Document, congress: Int, type: String, number: String) {
        try {
            // Process sponsors and cosponsors
            val cosponsorsResponse = congressApiService.getBillCosponsors(congress, type, number)
            processSponsors(document, cosponsorsResponse.cosponsors)
            
            // Process actions
            val actionsResponse = congressApiService.getBillActions(congress, type, number)
            processActions(document, actionsResponse.actions)
            
        } catch (e: Exception) {
            logger.error("Error processing detailed data for bill {}", document.billId, e)
        }
    }
    
    private fun processSponsors(document: Document, cosponsors: List<CongressCosponsor>) {
        for (cosponsor in cosponsors) {
            cosponsor.bioguideId?.let { bioguideId ->
                val sponsor = findOrCreateSponsor(cosponsor)
                val sponsorDate = cosponsor.sponsorshipDate?.let { 
                    LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) 
                }
                
                // Create document sponsor relationship
                // Note: This would need to be implemented with proper JPA relationships
                // For now, we'll log the sponsor data
                logger.debug("Processing sponsor: {} for document {}", bioguideId, document.billId)
            }
        }
    }
    
    private fun findOrCreateSponsor(congressCosponsor: CongressCosponsor): Sponsor {
        val bioguideId = congressCosponsor.bioguideId!!
        
        return sponsorRepository.findByBioguideId(bioguideId) ?: run {
            val newSponsor = Sponsor(
                bioguideId = bioguideId,
                firstName = congressCosponsor.firstName,
                lastName = congressCosponsor.lastName,
                party = congressCosponsor.party,
                state = congressCosponsor.state,
                district = congressCosponsor.district
            )
            sponsorRepository.save(newSponsor)
        }
    }
    
    private fun processActions(document: Document, actions: List<CongressDetailedAction>) {
        for (action in actions) {
            action.actionDate?.let { dateStr ->
                val actionDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                // Create document action
                // Note: This would need proper entity creation and saving
                logger.debug("Processing action for document {}: {}", document.billId, action.text)
            }
        }
    }
    
    private suspend fun triggerAiAnalysis(document: Document) {
        try {
            logger.info("Starting AI analysis for document: {}", document.billId)
            
            // Check if Ollama is available
            if (!ollamaService.isModelAvailable()) {
                logger.warn("Ollama model not available, skipping AI analysis for {}", document.billId)
                return
            }
            
            // Generate analyses
            val generalEffect = ollamaService.generateGeneralEffectAnalysis(
                document.title, 
                document.officialSummary
            )
            
            val economicEffect = ollamaService.generateEconomicEffectAnalysis(
                document.title, 
                document.officialSummary
            )
            
            val industryTags = ollamaService.generateIndustryTags(
                document.title, 
                document.officialSummary
            )
            
            // Save analysis
            val analysis = AiAnalysis(
                document = document,
                generalEffectText = generalEffect,
                economicEffectText = economicEffect,
                industryTags = industryTags.toTypedArray(),
                modelUsed = "0xroyce/plutus"
            )
            
            aiAnalysisRepository.save(analysis)
            logger.info("AI analysis completed for document: {}", document.billId)
            
        } catch (e: Exception) {
            logger.error("Error during AI analysis for document: {}", document.billId, e)
        }
    }
}
