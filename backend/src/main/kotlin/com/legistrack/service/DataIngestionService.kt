package com.legistrack.service

import com.legistrack.dto.external.CongressBill
import com.legistrack.dto.external.CongressCosponsor
import com.legistrack.dto.external.CongressDetailedAction
import com.legistrack.entity.AiAnalysis
import com.legistrack.entity.Document
import com.legistrack.entity.DocumentAction
import com.legistrack.entity.DocumentSponsor
import com.legistrack.entity.Sponsor
import com.legistrack.repository.AiAnalysisRepository
import com.legistrack.repository.DocumentActionRepository
import com.legistrack.repository.DocumentRepository
import com.legistrack.repository.DocumentSponsorRepository
import com.legistrack.repository.SponsorRepository
import com.legistrack.service.external.CongressApiService
import com.legistrack.service.external.OllamaService
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
    private val documentSponsorRepository: DocumentSponsorRepository,
    private val documentActionRepository: DocumentActionRepository,
    private val aiAnalysisRepository: AiAnalysisRepository,
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

    suspend fun processBill(congressBill: CongressBill): Boolean {
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
        val introductionDate =
            congressBill.introducedDate?.let {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
            }

        return Document(
            billId = billId,
            title = congressBill.title ?: "No title available",
            officialSummary = congressBill.summaries.firstOrNull()?.text,
            introductionDate = introductionDate,
            congressSession = congressBill.congress,
            billType = congressBill.type,
            status = congressBill.latestAction?.text,
        )
    }

    private suspend fun processDetailedBillData(
        document: Document,
        congress: Int,
        type: String,
        number: String,
    ) {
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

    private fun processSponsors(
        document: Document,
        cosponsors: List<CongressCosponsor>,
    ) {
        for ((index, cosponsor) in cosponsors.withIndex()) {
            cosponsor.bioguideId?.let { bioguideId ->
                val sponsor = findOrCreateSponsor(cosponsor)
                val sponsorDate =
                    cosponsor.sponsorshipDate?.let {
                        LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                    }

                // Create document sponsor relationship if it doesn't exist
                if (!documentSponsorRepository.existsByDocumentIdAndSponsorId(document.id!!, sponsor.id!!)) {
                    val documentSponsor =
                        DocumentSponsor(
                            document = document,
                            sponsor = sponsor,
                            isPrimarySponsor = index == 0, // First sponsor is typically primary
                            sponsorDate = sponsorDate,
                        )
                    documentSponsorRepository.save(documentSponsor)
                    logger.debug("Saved sponsor relationship: {} for document {}", bioguideId, document.billId)
                }
            }
        }
    }

    private fun findOrCreateSponsor(congressCosponsor: CongressCosponsor): Sponsor {
        val bioguideId = congressCosponsor.bioguideId!!

        return sponsorRepository.findByBioguideId(bioguideId) ?: run {
            val newSponsor =
                Sponsor(
                    bioguideId = bioguideId,
                    firstName = congressCosponsor.firstName,
                    lastName = congressCosponsor.lastName,
                    party = congressCosponsor.party,
                    state = congressCosponsor.state,
                    district = congressCosponsor.district,
                )
            sponsorRepository.save(newSponsor)
        }
    }

    private fun processActions(
        document: Document,
        actions: List<CongressDetailedAction>,
    ) {
        for (action in actions) {
            action.actionDate?.let { dateStr ->
                try {
                    val actionDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                    val actionText = action.text ?: "No action text available"

                    // Check if action already exists to avoid duplicates
                    if (!documentActionRepository.existsByDocumentIdAndActionDateAndActionText(
                            document.id!!,
                            actionDate,
                            actionText,
                        )
                    ) {
                        val documentAction =
                            DocumentAction(
                                document = document,
                                actionDate = actionDate,
                                actionType = action.type,
                                actionText = actionText,
                                chamber = action.chamber,
                                actionCode = action.actionCode,
                            )
                        documentActionRepository.save(documentAction)
                        logger.debug("Saved action for document {}: {}", document.billId, actionText)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse action date {} for document {}", dateStr, document.billId)
                }
            }
        }
    }

    /**
     * Refresh a single document's sponsors/actions from external sources and optionally re-run AI.
     */
    suspend fun refreshDocument(documentId: Long, reanalyze: Boolean = false) {
        val document = documentRepository.findById(documentId).orElse(null) ?: return

        // Parse identifiers from existing fields
        val type = document.billType ?: return
        val congress = document.congressSession ?: return
        val number = document.billId.substringAfter(type).substringBefore('-')

        try {
            val cosponsorsResponse = congressApiService.getBillCosponsors(congress, type, number)
            processSponsors(document, cosponsorsResponse.cosponsors)

            val actionsResponse = congressApiService.getBillActions(congress, type, number)
            processActions(document, actionsResponse.actions)
        } catch (e: Exception) {
            logger.error("Error refreshing document {}", document.billId, e)
        }

        if (reanalyze) {
            triggerAiAnalysis(document)
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
            val generalEffect =
                ollamaService.generateGeneralEffectAnalysis(
                    document.title,
                    document.officialSummary,
                )

            val economicEffect =
                ollamaService.generateEconomicEffectAnalysis(
                    document.title,
                    document.officialSummary,
                )

            val industryTags =
                ollamaService.generateIndustryTags(
                    document.title,
                    document.officialSummary,
                )

            // Persist only if we have meaningful content
            val hasContent =
                !generalEffect.isNullOrBlank() || !economicEffect.isNullOrBlank() || industryTags.isNotEmpty()

            if (hasContent) {
                val analysis =
                    AiAnalysis(
                        document = document,
                        generalEffectText = generalEffect,
                        economicEffectText = economicEffect,
                        industryTags = industryTags.toTypedArray(),
                        isValid = true,
                        modelUsed = "gpt-oss:20b",
                    )

                aiAnalysisRepository.save(analysis)
                logger.info("AI analysis completed for document: {}", document.billId)
            } else {
                logger.warn("AI analysis produced no content for document: {} — skipping save", document.billId)
            }
        } catch (e: Exception) {
            logger.error("Error during AI analysis for document: {}", document.billId, e)
        }
    }
}
