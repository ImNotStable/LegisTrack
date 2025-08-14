import { DocumentSummary, DocumentDetail, Page, ApiResponse } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

class ApiService {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;

    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error(`API request failed: ${endpoint}`, error);
      throw error;
    }
  }

  async getDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'introductionDate',
    sortDir: string = 'desc'
  ): Promise<Page<DocumentSummary>> {
    return this.request<Page<DocumentSummary>>(
      `/documents?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`
    );
  }

  async getDocumentById(id: string): Promise<DocumentDetail> {
    return this.request<DocumentDetail>(`/documents/${id}`);
  }

  async searchDocuments(
    query: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<DocumentSummary>> {
    const encodedQuery = encodeURIComponent(query);
    return this.request<Page<DocumentSummary>>(
      `/documents/search?q=${encodedQuery}&page=${page}&size=${size}`
    );
  }

  async getDocumentsByTag(
    tag: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<DocumentSummary>> {
    const encodedTag = encodeURIComponent(tag);
    return this.request<Page<DocumentSummary>>(
      `/documents/tag/${encodedTag}?page=${page}&size=${size}`
    );
  }

  async getAnalyticsSummary(): Promise<{
    totalDocuments: number;
    documentsWithAnalysis: number;
    avgDemocraticSponsorship: number;
    avgRepublicanSponsorship: number;
    topIndustryTags: Array<{ tag: string; count: number }>;
  }> {
    return this.request('/analytics/summary');
  }

  async refreshDocument(id: string): Promise<ApiResponse<DocumentDetail>> {
    return this.request<ApiResponse<DocumentDetail>>(`/documents/${id}/refresh`, {
      method: 'POST',
    });
  }

  async analyzeDocument(id: string): Promise<ApiResponse<DocumentDetail>> {
    return this.request<ApiResponse<DocumentDetail>>(`/documents/${id}/analyze`, {
      method: 'POST',
    });
  }
}

export const apiService = new ApiService();
export default apiService;
