import { DocumentSummary, DocumentDetail, Page, ApiResponse } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || process.env.REACT_APP_API_URL || '/api';

class ApiService {
	private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
		const url = `${API_BASE_URL}${endpoint}`;

		const method = (options.method || 'GET').toUpperCase();
		const headers: Record<string, string> = {
			...(options.headers as Record<string, string> | undefined),
		};
		if (method !== 'GET' && method !== 'HEAD') {
			headers['Content-Type'] = headers['Content-Type'] || 'application/json';
		}

		const config: RequestInit = { ...options, headers };

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
		page = 0,
		size = 20,
		sortBy = 'introductionDate',
		sortDir = 'desc'
	): Promise<Page<DocumentSummary>> {
		// Sanitize page input to avoid NaN or negative values propagating to API
		const safePage = Number.isFinite(page) && page >= 0 ? Math.floor(page) : 0;
		const safeSize = Number.isFinite(size) && size > 0 ? Math.min(Math.floor(size), 100) : 20;
		return this.request<Page<DocumentSummary>>(
			`/documents?page=${safePage}&size=${safeSize}&sortBy=${encodeURIComponent(sortBy)}&sortDir=${encodeURIComponent(sortDir)}`
		);
	}

	async getDocumentById(id: string): Promise<DocumentDetail> {
		return this.request<DocumentDetail>(`/documents/${id}`);
	}

	async searchDocuments(query: string, page = 0, size = 20): Promise<Page<DocumentSummary>> {
		const encodedQuery = encodeURIComponent(query);
		return this.request<Page<DocumentSummary>>(
			`/documents/search?q=${encodedQuery}&page=${page}&size=${size}`
		);
	}

	async getDocumentsByTag(tag: string, page = 0, size = 20): Promise<Page<DocumentSummary>> {
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
