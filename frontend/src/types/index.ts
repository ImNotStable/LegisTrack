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
