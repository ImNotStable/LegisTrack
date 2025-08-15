import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { DocumentSummary, DocumentDetail, Page } from '../types';

// Mock data
const mockDocumentSummary: DocumentSummary = {
  id: 1,
  billId: 'H.R.1234',
  title: 'Test Bill for Healthcare Reform',
  introductionDate: '2024-01-15',
  status: 'introduced',
  industryTags: ['Healthcare', 'Insurance', 'Public Health'],
  partyBreakdown: {
    democratic: 25,
    republican: 10,
    independent: 2,
    other: 0,
    total: 37,
    democraticPercentage: 67.6,
    republicanPercentage: 27.0,
  },
  hasValidAnalysis: true,
};

const mockDocumentDetail: DocumentDetail = {
  id: 1,
  billId: 'H.R.1234',
  title: 'Test Bill for Healthcare Reform',
  officialSummary: 'This bill addresses healthcare reform and insurance coverage.',
  introductionDate: '2024-01-15',
  congressSession: 118,
  billType: 'H.R.',
  fullTextUrl: 'https://congress.gov/bill/test',
  status: 'introduced',
  sponsors: [
    {
      id: 1,
      bioguideId: 'S000001',
      firstName: 'John',
      lastName: 'Doe',
      party: 'Democratic',
      state: 'CA',
      district: '12',
      isPrimarySponsor: true,
      sponsorDate: '2024-01-15',
    },
  ],
  actions: [
    {
      id: 1,
      actionDate: '2024-01-15',
      actionType: 'introduction',
      actionText: 'Introduced in House',
      chamber: 'House',
      actionCode: 'Intro-H',
    },
  ],
  analysis: {
    id: 1,
    generalEffectText: 'This bill would significantly impact healthcare coverage.',
    economicEffectText: 'Expected to reduce healthcare costs by 15%.',
    industryTags: ['Healthcare', 'Insurance'],
    isValid: true,
    analysisDate: '2024-01-16',
    modelUsed: 'llama2',
  },
  partyBreakdown: {
    democratic: 25,
    republican: 10,
    independent: 2,
    other: 0,
    total: 37,
    democraticPercentage: 67.6,
    republicanPercentage: 27.0,
  },
  createdAt: '2024-01-15T10:00:00Z',
  updatedAt: '2024-01-16T12:00:00Z',
};

const mockPage: Page<DocumentSummary> = {
  content: [mockDocumentSummary],
  pageable: {
    pageNumber: 0,
    pageSize: 20,
    sort: { empty: false, sorted: true, unsorted: false },
    offset: 0,
    unpaged: false,
    paged: true,
  },
  last: true,
  totalPages: 1,
  totalElements: 1,
  size: 20,
  number: 0,
  sort: { empty: false, sorted: true, unsorted: false },
  first: true,
  numberOfElements: 1,
  empty: false,
};

// API handlers
export const handlers = [
  rest.get('/api/documents', (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockPage));
  }),

  rest.get('/api/documents/:id', (req, res, ctx) => {
    const { id } = req.params;
    if (id === '1') {
      return res(ctx.status(200), ctx.json(mockDocumentDetail));
    }
    return res(ctx.status(404), ctx.json({ message: 'Document not found' }));
  }),

  rest.post('/api/documents/:id/refresh', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ success: true, message: 'Document refreshed', data: mockDocumentDetail })
    );
  }),

  rest.post('/api/documents/:id/analyze', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ success: true, message: 'Document analyzed', data: mockDocumentDetail })
    );
  }),

  rest.get('/api/documents/search', (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockPage));
  }),

  rest.get('/api/analytics/summary', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        totalDocuments: 100,
        documentsWithAnalysis: 85,
        avgDemocraticSponsorship: 65.5,
        avgRepublicanSponsorship: 32.8,
        topIndustryTags: [
          { tag: 'Healthcare', count: 25 },
          { tag: 'Technology', count: 20 },
          { tag: 'Environment', count: 15 },
        ],
      })
    );
  }),
];

// Setup mock server
export const server = setupServer(...handlers);
