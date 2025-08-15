export interface DocumentSummary {
  id: number;
  billId: string;
  title: string;
  introductionDate?: string;
  status?: string;
  industryTags: string[];
  partyBreakdown: PartyBreakdown;
  hasValidAnalysis: boolean;
}

export interface DocumentDetail {
  id: number;
  billId: string;
  title: string;
  officialSummary?: string;
  introductionDate?: string;
  congressSession?: number;
  billType?: string;
  fullTextUrl?: string;
  status?: string;
  sponsors: Sponsor[];
  actions: DocumentAction[];
  analysis?: AiAnalysis;
  partyBreakdown: PartyBreakdown;
  createdAt: string;
  updatedAt: string;
}

export interface Sponsor {
  id: number;
  bioguideId: string;
  firstName?: string;
  lastName?: string;
  party?: string;
  state?: string;
  district?: string;
  isPrimarySponsor: boolean;
  sponsorDate?: string;
}

export interface DocumentAction {
  id: number;
  actionDate: string;
  actionType?: string;
  actionText: string;
  chamber?: string;
  actionCode?: string;
}

export interface AiAnalysis {
  id: number;
  generalEffectText?: string;
  economicEffectText?: string;
  industryTags: string[];
  isValid: boolean;
  analysisDate: string;
  modelUsed?: string;
}

export interface PartyBreakdown {
  democratic: number;
  republican: number;
  independent: number;
  other: number;
  total: number;
  democraticPercentage: number;
  republicanPercentage: number;
}

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    unpaged: boolean;
    paged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface Document {
  id: string;
  title: string;
  summary: string;
  congress: number;
  type: string;
  number: string;
  originChamber: string;
  introducedDate: string;
  latestActionDate: string;
  latestActionText: string;
  url: string;
  sponsorName?: string;
  sponsorParty?: string;
  sponsorState?: string;
  subjects: string[];
  aiSummary?: string;
  aiKeyPoints?: string[];
  impactScore?: number;
  controversyLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface ApiError {
  message: string;
  status?: number;
  timestamp?: string;
}

export interface DocumentFilters {
  congress?: number;
  type?: string;
  chamber?: string;
  subject?: string;
  sponsor?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface SearchParams {
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  filters?: DocumentFilters;
}
