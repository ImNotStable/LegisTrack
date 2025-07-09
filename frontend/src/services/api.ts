import { DocumentSummary, Page, ApiResponse } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/api';

class ApiService {
  private async fetchWithErrorHandling<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'introductionDate',
    sortDir: string = 'desc'
  ): Promise<Page<DocumentSummary>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDir,
    });

    return this.fetchWithErrorHandling<Page<DocumentSummary>>(
      `/documents?${params}`
    );
  }

  async triggerDataIngestion(fromDate?: string): Promise<ApiResponse<any>> {
    const params = fromDate ? `?fromDate=${fromDate}` : '';
    return this.fetchWithErrorHandling<ApiResponse<any>>(`/documents/ingest${params}`, {
      method: 'POST',
    });
  }
}

export const apiService = new ApiService();
