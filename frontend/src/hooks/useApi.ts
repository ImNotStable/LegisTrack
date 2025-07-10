import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '../services/api';
import { DocumentSummary, DocumentDetail, Page } from '../types';

export const useDocuments = (
  page: number = 0,
  size: number = 20,
  sortBy: string = 'introductionDate',
  sortDir: string = 'desc'
) => {
  return useQuery<Page<DocumentSummary>>({
    queryKey: ['documents', page, size, sortBy, sortDir],
    queryFn: () => apiService.getDocuments(page, size, sortBy, sortDir),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const useDocumentById = (id: number) => {
  return useQuery<DocumentDetail>({
    queryKey: ['document', id],
    queryFn: () => apiService.getDocumentById(id),
    staleTime: 5 * 60 * 1000, // 5 minutes
    enabled: !!id, // Only run query if id is provided
  });
};

export const useTriggerDataIngestion = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (fromDate?: string) => apiService.triggerDataIngestion(fromDate),
    onSuccess: () => {
      // Invalidate all document queries after ingestion
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });
};
