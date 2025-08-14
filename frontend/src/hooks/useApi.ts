import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '../services/api';
import { DocumentSummary, DocumentDetail, Page } from '../types';

// Query keys for consistent caching
export const queryKeys = {
  documents: (page: number, size: number, sortBy: string, sortDir: string) =>
    ['documents', page, size, sortBy, sortDir],
  document: (id: string) => ['document', id],
  search: (query: string, page: number, size: number) =>
    ['search', query, page, size],
  documentsByTag: (tag: string, page: number, size: number) =>
    ['documentsByTag', tag, page, size],
  analytics: () => ['analytics'],
};

// Hook for fetching paginated documents
export const useDocuments = (
  page: number = 0,
  size: number = 20,
  sortBy: string = 'introductionDate',
  sortDir: string = 'desc'
) => {
  return useQuery<Page<DocumentSummary>, Error>({
    queryKey: queryKeys.documents(page, size, sortBy, sortDir),
    queryFn: () => apiService.getDocuments(page, size, sortBy, sortDir),
    staleTime: 5 * 60 * 1000, // 5 minutes
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
export const useSearchDocuments = (
  query: string,
  page: number = 0,
  size: number = 20
) => {
  return useQuery<Page<DocumentSummary>, Error>({
    queryKey: queryKeys.search(query, page, size),
    queryFn: () => apiService.searchDocuments(query, page, size),
    enabled: !!query && query.length > 2,
    staleTime: 2 * 60 * 1000, // 2 minutes
    retry: process.env.NODE_ENV === 'test' ? 0 : 1,
  });
};

// Hook for fetching documents by tag
export const useDocumentsByTag = (
  tag: string,
  page: number = 0,
  size: number = 20
) => {
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
      queryClient.invalidateQueries({ queryKey: ['documents'] });
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
