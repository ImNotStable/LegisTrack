import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';

import { apiService } from '../services/api';
import { DocumentSummary, DocumentDetail, Page } from '../types';

// Query keys for consistent caching
export const queryKeys = {
	documents: (page: number, size: number, sortBy: string, sortDir: string) => [
		'documents',
		page,
		size,
		sortBy,
		sortDir,
	],
	document: (id: string) => ['document', id],
	search: (query: string, page: number, size: number) => ['search', query, page, size],
	documentsByTag: (tag: string, page: number, size: number) => ['documentsByTag', tag, page, size],
	analytics: () => ['analytics'],
};

// Hook for fetching paginated documents
export const useDocuments = (
	page = 0,
	size = 20,
	sortBy = 'introductionDate',
	sortDir = 'desc'
) => {
	return useQuery<Page<DocumentSummary>, Error>({
		queryKey: queryKeys.documents(page, size, sortBy, sortDir),
		queryFn: () => apiService.getDocuments(page, size, sortBy, sortDir),
		staleTime: 5 * 60 * 1000, // 5 minutes
		retry: process.env.NODE_ENV === 'test' ? 0 : 2,
	});
};

// Infinite documents for progressive loading
export const useInfiniteDocuments = (size = 20, sortBy = 'introductionDate', sortDir = 'desc') => {
	return useInfiniteQuery<Page<DocumentSummary>, Error>({
		queryKey: ['documents-infinite', size, sortBy, sortDir],
		initialPageParam: 0,
		queryFn: ({ pageParam }) => {
			const raw = pageParam as number;
			const safe = Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0;
			return apiService.getDocuments(safe, size, sortBy, sortDir);
		},
		getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
		staleTime: 5 * 60 * 1000,
		retry: process.env.NODE_ENV === 'test' ? 0 : 2,
	});
};

// Hook for fetching a single document
export const useDocument = (id: string) => {
	return useQuery<DocumentDetail, Error>({
		queryKey: queryKeys.document(id),
		queryFn: () => apiService.getDocumentById(id),
		enabled: !!id,
		staleTime: 10 * 60 * 1000, // 10 minutes
		retry: process.env.NODE_ENV === 'test' ? 0 : 2,
	});
};

// Hook for searching documents
export const useSearchDocuments = (query: string, page = 0, size = 20) => {
	return useQuery<Page<DocumentSummary>, Error>({
		queryKey: queryKeys.search(query, page, size),
		queryFn: () => apiService.searchDocuments(query, page, size),
		enabled: !!query && query.length > 2,
		staleTime: 2 * 60 * 1000, // 2 minutes
		retry: process.env.NODE_ENV === 'test' ? 0 : 1,
	});
};

// Hook for fetching documents by tag
export const useDocumentsByTag = (tag: string, page = 0, size = 20) => {
	return useQuery<Page<DocumentSummary>, Error>({
		queryKey: queryKeys.documentsByTag(tag, page, size),
		queryFn: () => apiService.getDocumentsByTag(tag, page, size),
		enabled: !!tag,
		staleTime: 5 * 60 * 1000, // 5 minutes
		retry: process.env.NODE_ENV === 'test' ? 0 : 2,
	});
};

// Hook for analytics data
export const useAnalytics = () => {
	return useQuery({
		queryKey: queryKeys.analytics(),
		queryFn: () => apiService.getAnalyticsSummary(),
		staleTime: 15 * 60 * 1000, // 15 minutes
		retry: process.env.NODE_ENV === 'test' ? 0 : 2,
	});
};

// Mutation hooks for data modification
export const useRefreshDocument = () => {
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: (id: string) => apiService.refreshDocument(id),
		onSuccess: (data, id) => {
			// Invalidate and refetch the specific document
			queryClient.invalidateQueries({ queryKey: queryKeys.document(id) });
			// Also invalidate the documents list to show updated data
			queryClient.invalidateQueries({
				predicate: (q) =>
					Array.isArray(q.queryKey) &&
					(q.queryKey[0] === 'documents' || q.queryKey[0] === 'documents-infinite'),
			});
		},
	});
};

export const useAnalyzeDocument = () => {
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: (id: string) => apiService.analyzeDocument(id),
		onSuccess: (data, id) => {
			// Invalidate and refetch the specific document
			queryClient.invalidateQueries({ queryKey: queryKeys.document(id) });
			// Also invalidate analytics data
			queryClient.invalidateQueries({ queryKey: queryKeys.analytics() });
		},
	});
};

// Custom hook for optimistic updates
export const useOptimisticUpdate = () => {
	const queryClient = useQueryClient();

	const updateDocument = (id: string, updater: (old: DocumentDetail) => DocumentDetail) => {
		queryClient.setQueryData(queryKeys.document(id), updater);
	};

	return { updateDocument };
};
