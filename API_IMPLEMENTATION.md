# LegisTrack API Implementation Documentation

## Overview

LegisTrack now provides comprehensive support for both **Congress.gov API** and **GovInfo API**, enabling full access to U.S. legislative data from both official government sources.

## Congress.gov API Implementation

The `CongressApiService` provides complete access to all Congress.gov API endpoints with the following features:

### Supported Endpoints

#### Bills
- `getRecentBills()` - Fetch recent bills with date filtering
- `getBillDetails()` - Get detailed bill information
- `getBillCosponsors()` - Retrieve bill co-sponsors
- `getBillActions()` - Get legislative actions for a bill
- `getBillSummaries()` - Fetch official bill summaries
- `getBillTextVersions()` - Get available text versions

#### Amendments
- `getAmendments()` - List amendments by congress
- `getAmendmentDetails()` - Get detailed amendment information

#### Members
- `getMembers()` - List Congress members
- `getMemberDetails()` - Get detailed member information

#### Committee Reports
- `getCommitteeReports()` - Fetch committee reports

#### Nominations & Treaties
- `getNominations()` - Get presidential nominations
- `getTreaties()` - Fetch treaty information

### Key Features
- ✅ **Comprehensive Caching** - All endpoints use `@Cacheable` for optimal performance
- ✅ **Retry Logic** - Automatic retry with exponential backoff for resilience
- ✅ **Proper Error Handling** - Graceful degradation with meaningful error logging
- ✅ **Rate Limiting Aware** - Built for Congress.gov API rate limits
- ✅ **Flexible Pagination** - Support for offset-based pagination
- ✅ **Type Safety** - Full Kotlin data classes for all responses

### Configuration
```properties
app.congress.api.key=${CONGRESS_API_KEY}
app.congress.api.base-url=https://api.congress.gov/v3
```

## GovInfo API Implementation

The `GovInfoApiService` provides complete access to all GovInfo.gov API endpoints:

### Supported Endpoints

#### Collections & Packages
- `getCollections()` - List all available collections
- `getPackages()` - Get packages by collection and date range
- `getPackageDetails()` - Detailed package metadata
- `getPackageGranules()` - Sub-documents within packages
- `getGranuleDetails()` - Granule-level metadata

#### Publication & Discovery
- `getPublishedPackages()` - Find documents by publication date
- `getRelatedDocuments()` - Discover document relationships
- `searchDocuments()` - Full-text search with field operators

#### Bills-Specific
- `getBills()` - Filter BILLS collection by congress/type
- `getBillStatus()` - Access BILLSTATUS XML data
- `downloadContent()` - Download documents in multiple formats

### Key Features
- ✅ **Multi-Format Support** - Access to TXT, XML, PDF, ZIP formats
- ✅ **Advanced Search** - Query DSL with field operators and sorting
- ✅ **Relationship Discovery** - Find related documents across collections
- ✅ **Granular Access** - Package and granule-level data access
- ✅ **Robust Pagination** - OffsetMark-based pagination system
- ✅ **Content Download** - Direct binary content retrieval

### Supported Collections
- **BILLS** - Congressional bills and resolutions
- **BILLSTATUS** - Detailed bill status and metadata
- **FR** - Federal Register documents
- **CFR** - Code of Federal Regulations
- **CRPT** - Committee reports
- **CHRG** - Committee hearings
- **PLAW** - Public laws
- **STATUTE** - Statutes at Large
- And many more...

### Configuration
```properties
app.govinfo.api.key=${GOVINFO_API_KEY}
app.govinfo.api.base-url=https://api.govinfo.gov
```

## Data Transfer Objects (DTOs)

### Congress.gov DTOs
All DTOs are located in `com.legistrack.dto.external.CongressApiDtos`:

- `CongressBillsResponse` - Bills listing with pagination
- `CongressBill` - Detailed bill information
- `CongressAmendment` - Amendment details
- `CongressMember` - Congress member information
- `CongressReport` - Committee report data
- `CongressNomination` - Presidential nomination details
- `CongressTreaty` - Treaty information
- Plus supporting classes for sponsors, actions, summaries, etc.

### GovInfo DTOs
All DTOs are located in `com.legistrack.dto.external.GovInfoApiDtos`:

- `GovInfoPackagesResponse` - Package listings
- `GovInfoPackageDetailsResponse` - Detailed package metadata
- `GovInfoSearchResponse` - Search results
- `GovInfoBillStatus` - Bill status details
- `GovInfoRelatedResponse` - Document relationships
- Plus supporting classes for granules, downloads, collections, etc.

## Usage Examples

### Congress.gov API Usage
```kotlin
@Service
class BillService(private val congressApi: CongressApiService) {
    
    suspend fun getRecentBills(): List<CongressBill> {
        val response = congressApi.getRecentBills(
            fromDate = LocalDate.now().minusDays(30),
            limit = 50
        )
        return response.bills
    }
    
    suspend fun getBillWithFullDetails(congress: Int, type: String, number: String): BillDetails? {
        val bill = congressApi.getBillDetails(congress, type, number) ?: return null
        val cosponsors = congressApi.getBillCosponsors(congress, type, number)
        val actions = congressApi.getBillActions(congress, type, number)
        val summaries = congressApi.getBillSummaries(congress, type, number)
        
        return BillDetails(bill, cosponsors, actions, summaries)
    }
}
```

### GovInfo API Usage
```kotlin
@Service
class DocumentService(private val govInfoApi: GovInfoApiService) {
    
    suspend fun searchBills(query: String): List<GovInfoSearchResult> {
        val response = govInfoApi.searchDocuments(
            query = "collection:BILLS AND $query",
            pageSize = 20,
            sortField = "publishdate",
            sortOrder = "DESC"
        )
        return response.packages
    }
    
    suspend fun downloadBillText(packageId: String): ByteArray? {
        return govInfoApi.downloadContent(packageId, "txt")
    }
    
    suspend fun findRelatedDocuments(billPackageId: String): GovInfoRelatedResponse {
        return govInfoApi.getRelatedDocuments(billPackageId, "BILLS")
    }
}
```

## Performance Optimizations

### Caching Strategy
- **Congress.gov**: TTL-based caching with keys including all parameters
- **GovInfo**: Similar caching with collection-specific strategies
- **Redis Backend**: All caches use Redis for distributed caching

### Retry Logic
- **Exponential Backoff**: 3 attempts with 2-second delays
- **Circuit Breaker**: Graceful degradation on service failures
- **Timeout Handling**: Reasonable timeouts for large responses

### Rate Limiting
- **Congress.gov**: Respects 5000 requests/hour limit
- **GovInfo**: Handles API.data.gov rate limiting
- **Request Batching**: Efficient bulk operations where possible

## Error Handling

Both services implement comprehensive error handling:

```kotlin
try {
    // API call
} catch (e: WebClientResponseException) {
    logger.error("API responded with error: ${e.statusCode}")
    // Return empty response or null
} catch (e: TimeoutException) {
    logger.error("API request timed out")
    // Return cached data if available
} catch (e: Exception) {
    logger.error("Unexpected error", e)
    // Graceful degradation
}
```

## Environment Setup

### Required Environment Variables
```bash
# Congress.gov API (register at https://api.congress.gov/)
CONGRESS_API_KEY=your_congress_api_key_here

# GovInfo API (register at https://api.data.gov/)
GOVINFO_API_KEY=your_govinfo_api_key_here
```

### API Key Registration
1. **Congress.gov API**: Visit https://api.congress.gov/ and sign up
2. **GovInfo API**: Register at https://api.data.gov/ and request GovInfo access

## Monitoring & Observability

Both services include comprehensive logging:
- **Request/Response Logging**: Debug-level logging for all API calls
- **Performance Metrics**: Timing and cache hit/miss ratios
- **Error Tracking**: Detailed error context and stack traces
- **Health Checks**: Endpoint availability monitoring

## Future Enhancements

### Planned Features
- [ ] **Webhook Support**: Real-time updates from APIs
- [ ] **Bulk Operations**: Efficient batch processing
- [ ] **Advanced Filtering**: Client-side filtering capabilities
- [ ] **Content Parsing**: Automatic text extraction from PDFs
- [ ] **ML Integration**: Enhanced document classification

### API Improvements
- [ ] **GraphQL Layer**: Unified query interface
- [ ] **Real-time Subscriptions**: WebSocket-based updates
- [ ] **Content Caching**: Full document caching strategy
- [ ] **Search Enhancement**: Elasticsearch integration

## Troubleshooting

### Common Issues
1. **Rate Limiting**: Implement exponential backoff and respect limits
2. **Large Responses**: Use streaming for large document downloads
3. **Cache Misses**: Monitor cache hit ratios and adjust TTLs
4. **API Timeouts**: Increase timeout values for complex queries

### Debug Mode
Enable debug logging for detailed request/response information:
```properties
logging.level.com.legistrack.service.external=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

## API Documentation Links

- **Congress.gov API**: https://github.com/LibraryOfCongress/api.congress.gov
- **GovInfo API**: https://github.com/usgpo/api
- **GovInfo Swagger**: https://api.govinfo.gov/govinfoapi/api-docs

---

This implementation provides LegisTrack with comprehensive access to official U.S. government legislative data, enabling powerful tracking, analysis, and reporting capabilities for legislative documents and proceedings.
